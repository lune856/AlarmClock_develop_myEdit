package com.theglendales.alarm;

import android.content.ContentResolver;

import com.theglendales.alarm.background.Event;
import com.theglendales.alarm.configuration.Prefs;
import com.theglendales.alarm.configuration.Store;
import com.theglendales.alarm.interfaces.Alarm;
import com.theglendales.alarm.interfaces.IAlarmsManager;
import com.theglendales.alarm.interfaces.Intents;
import com.theglendales.alarm.logger.Logger;
import com.theglendales.alarm.logger.SysoutLogWriter;
import com.theglendales.alarm.model.AlarmCore;
import com.theglendales.alarm.model.AlarmCoreFactory;
import com.theglendales.alarm.model.AlarmStore;
import com.theglendales.alarm.model.AlarmValue;
import com.theglendales.alarm.model.Alarms;
import com.theglendales.alarm.model.AlarmsScheduler;
import com.theglendales.alarm.model.CalendarType;
import com.theglendales.alarm.model.Calendars;
import com.theglendales.alarm.model.ContainerFactory;
import com.theglendales.alarm.model.DaysOfWeek;
import com.theglendales.alarm.persistance.DatabaseQuery;
import com.theglendales.alarm.stores.InMemoryRxDataStoreFactory;
import com.theglendales.alarm.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AlarmsTest {
    private AlarmCore.IStateNotifier stateNotifierMock;
    private final AlarmSchedulerTest.SetterMock alarmSetterMock = new AlarmSchedulerTest.SetterMock();
    private TestScheduler testScheduler;
    private Store store;
    private Prefs prefs;
    private Logger logger;
    private int currentHour = 0;
    private int currentMinute = 0;
    private final Calendars calendars = new Calendars() {
        @Override
        public Calendar now() {
            Calendar instance = Calendar.getInstance();
            instance.set(Calendar.HOUR_OF_DAY, currentHour);
            instance.set(Calendar.MINUTE, currentMinute);
            return instance;
        }
    };
    private final TestContainerFactory containerFactory = new TestContainerFactory(calendars);
    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("---- " + description.getMethodName() + " ----");
        }
    };

    @Before
    public void setUp() {
        testScheduler = new TestScheduler();
        logger = Logger.create(new SysoutLogWriter());

        prefs = Prefs.create(Single.just(true), InMemoryRxDataStoreFactory.create());

        store = new Store(
                /* alarmsSubject */ BehaviorSubject.<List<AlarmValue>>createDefault(new ArrayList<AlarmValue>()),
                /* next */ BehaviorSubject.createDefault(Optional.<Store.Next>absent()),
                /* sets */ PublishSubject.<Store.AlarmSet>create(),
                /* events */ PublishSubject.<Event>create());

        stateNotifierMock = mock(AlarmCore.IStateNotifier.class);
    }

    private Alarms createAlarms(DatabaseQuery query) {
        AlarmsScheduler alarmsScheduler = new AlarmsScheduler(alarmSetterMock, logger, store, prefs, calendars);
        Alarms alarms = new Alarms(alarmsScheduler, query, new AlarmCoreFactory(logger,
                alarmsScheduler,
                stateNotifierMock,
                prefs,
                store,
                calendars
        ),
                containerFactory,
                logger);
        alarmsScheduler.start();
        return alarms;
    }

    private Alarms createAlarms() {
        return createAlarms(mockQuery());
    }

    @androidx.annotation.NonNull
    private DatabaseQuery mockQuery() {
        final DatabaseQuery query = mock(DatabaseQuery.class);
        List<AlarmStore> list = new ArrayList<>();
        when(query.query()).thenReturn(Single.just(list));
        return query;
    }

    @Test
    public void create() {
        //when
        IAlarmsManager instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.enable(true);
        testScheduler.triggerActions();
        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 1 && alarmValues.get(0).isEnabled();
            }
        });
    }

    @Test
    public void deleteDisabledAlarm() {
        //when
        IAlarmsManager instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        testScheduler.triggerActions();
        instance.delete(newAlarm);
        testScheduler.triggerActions();
        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 0;
            }
        });
    }

    @Test
    public void deleteEnabledAlarm() {
        //when
        IAlarmsManager instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        testScheduler.triggerActions();
        newAlarm.enable(true);
        testScheduler.triggerActions();
        instance.getAlarm(0).delete();
        testScheduler.triggerActions();
        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 0;
            }
        });
    }

    @Test
    public void createThreeAlarms() {
        //when
        IAlarmsManager instance = createAlarms();
        instance.createNewAlarm();
        testScheduler.triggerActions();
        instance.createNewAlarm().enable(true);
        testScheduler.triggerActions();
        instance.createNewAlarm();
        testScheduler.triggerActions();
        //verify
        store.alarms().test().assertValueAt(0, new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                System.out.println(alarmValues);
                return alarmValues.size() == 3
                        && !alarmValues.get(0).isEnabled()
                        && alarmValues.get(1).isEnabled()
                        && !alarmValues.get(2).isEnabled();
            }
        });
    }

    static class DatabaseQueryMock extends DatabaseQuery {
        private ContainerFactory factory;

        public DatabaseQueryMock(ContainerFactory factory) {
            super(mock(ContentResolver.class), factory);
            this.factory = factory;
        }

        @Override
        public Single<List<AlarmStore>> query() {
            AlarmStore container = factory.create();
            container.setValue(container.getValue()
                    .withId(100500)
                    .withIsEnabled(true)
                    .withLabel("hello")
            );
            List<AlarmStore> item = CollectionsKt.arrayListOf(container);
            return Single.just(item);
        }
    }

    @Test
    public void alarmsFromMemoryMustBePresentInTheList() {
        //when
        Alarms instance = createAlarms(new DatabaseQueryMock(new TestContainerFactory(new Calendars() {
            @Override
            public Calendar now() {
                return Calendar.getInstance();
            }
        })));

        instance.start();

        //verify
        store.alarms().test()
                .assertValue(new Predicate<List<AlarmValue>>() {
                    @Override
                    public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                        System.out.println(alarmValues);
                        return alarmValues.size() == 1
                                && alarmValues.get(0).isEnabled()
                                && alarmValues.get(0).getLabel().equals("hello");
                    }
                });
    }

    @Test
    public void editAlarm() {
        //when
        IAlarmsManager instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();

        newAlarm.edit(new Function1<AlarmValue, AlarmValue>() {
                          @Override
                          public AlarmValue invoke(AlarmValue alarmValue) {
                              return alarmValue
                                      .withIsEnabled(true)
                                      .withHour(7);
                          }
                      }
        );

        testScheduler.triggerActions();
        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 1
                        && alarmValues.get(0).isEnabled()
                        && alarmValues.get(0).getHour() == 7;
            }
        });
    }

    @Test
    public void firedAlarmShouldBeDisabledIfNoRepeatingIsSet() {
        //when
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.enable(true);
        testScheduler.triggerActions();

        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        newAlarm.dismiss();
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_DISMISS_ACTION));

        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 1
                        && !alarmValues.get(0).isEnabled();
            }
        });
    }

    @Test
    public void firedAlarmShouldBeRescheduledIfRepeatingIsSet() {
        //when
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.edit(new Function1<AlarmValue, AlarmValue>() {
                          @Override
                          public AlarmValue invoke(AlarmValue alarmValue) {
                              return alarmValue
                                      .withIsEnabled(true)
                                      .withDaysOfWeek(new DaysOfWeek(1));
                          }
                      }
        );

        testScheduler.triggerActions();

        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        newAlarm.dismiss();
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_DISMISS_ACTION));

        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 1
                        && alarmValues.get(0).isEnabled();
            }
        });
    }

    @Test
    public void changingAlarmWhileItIsFiredShouldReschedule() {
        //when
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.enable(true);
        testScheduler.triggerActions();

        assertThat(alarmSetterMock.getTypeName()).isEqualTo("NORMAL");

        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        newAlarm.edit(new Function1<AlarmValue, AlarmValue>() {
                          @Override
                          public AlarmValue invoke(AlarmValue alarmValue) {
                              return alarmValue
                                      .withDaysOfWeek(new DaysOfWeek(1))
                                      .withIsPrealarm(true);
                          }
                      }
        );

        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_DISMISS_ACTION));

        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 1
                        && alarmValues.get(0).isEnabled();
            }
        });

        assertThat(alarmSetterMock.getId()).isEqualTo(newAlarm.getId());
    }


    @Test
    public void firedAlarmShouldBeStillEnabledAfterSnoozed() {
        //given
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        //TODO circle the time, otherwise the tests may fail around 0 hours
        newAlarm.edit(new Function1<AlarmValue, AlarmValue>() {
                          @Override
                          public AlarmValue invoke(AlarmValue alarmValue) {
                              return alarmValue
                                      .withIsEnabled(true)
                                      .withHour(0)
                                      .withDaysOfWeek(new DaysOfWeek(1))
                                      .withIsPrealarm(true);
                          }
                      }
        );
        testScheduler.triggerActions();
        //TODO verify

        //when pre-alarm fired
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.PREALARM);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_PREALARM_ACTION));

        //when pre-alarm-snoozed
        newAlarm.snooze();
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_SNOOZE_ACTION), any());

        //when alarm fired
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ACTION_CANCEL_SNOOZE));
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        //when alarm is snoozed
        newAlarm.snooze();
        testScheduler.triggerActions();
        verify(stateNotifierMock, times(2)).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_DISMISS_ACTION));

        newAlarm.delete();
        verify(stateNotifierMock, times(2)).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ACTION_CANCEL_SNOOZE));
    }


    @Test
    public void snoozeToTime() {
        //given
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        //TODO circle the time, otherwise the tests may fail around 0 hours
        newAlarm.edit(new Function1<AlarmValue, AlarmValue>() {
                          @Override
                          public AlarmValue invoke(AlarmValue alarmValue) {
                              return alarmValue
                                      .withIsEnabled(true)
                                      .withHour(0)
                                      .withDaysOfWeek(new DaysOfWeek(1));
                          }
                      }
        );
        testScheduler.triggerActions();
        //TODO verify

        //when alarm fired
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        //when pre-alarm-snoozed
        newAlarm.snooze(23, 59);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_SNOOZE_ACTION), any());
    }

    @Test
    public void snoozePreAlarmToTime() {
        //given
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        //TODO circle the time, otherwise the tests may fail around 0 hours
        newAlarm.edit(new Function1<AlarmValue, AlarmValue>() {
                          @Override
                          public AlarmValue invoke(AlarmValue alarmValue) {
                              return alarmValue
                                      .withIsEnabled(true)
                                      .withHour(0)
                                      .withDaysOfWeek(new DaysOfWeek(1))
                                      .withIsPrealarm(true);
                          }
                      }
        );
        testScheduler.triggerActions();
        //TODO verify

        //when alarm fired
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.PREALARM);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_PREALARM_ACTION));

        //when pre-alarm-snoozed
        newAlarm.snooze(23, 59);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_SNOOZE_ACTION), any());

        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.SNOOZE);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));
    }


    @Test
    public void prealarmTimedOutAndThenDisabled() {
        //given
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        //TODO circle the time, otherwise the tests may fail around 0 hours
        newAlarm.edit(new Function1<AlarmValue, AlarmValue>() {
                          @Override
                          public AlarmValue invoke(AlarmValue alarmValue) {
                              return alarmValue
                                      .withIsEnabled(true)
                                      .withHour(0)
                                      .withDaysOfWeek(new DaysOfWeek(1))
                                      .withIsPrealarm(true);
                          }
                      }
        );
        testScheduler.triggerActions();
        //TODO verify

        //when alarm fired
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.PREALARM);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_PREALARM_ACTION));

        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        //when pre-alarm-snoozed
        newAlarm.enable(false);
        testScheduler.triggerActions();
        verify(stateNotifierMock, atLeastOnce()).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_DISMISS_ACTION));
    }

    @Test
    public void snoozedAlarmsMustGoOutOfHibernation() {
        //given
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.edit(new Function1<AlarmValue, AlarmValue>() {
                          @Override
                          public AlarmValue invoke(AlarmValue alarmValue) {
                              return alarmValue
                                      .withIsEnabled(true)
                                      .withHour(0)
                                      .withDaysOfWeek(new DaysOfWeek(0))
                                      .withIsPrealarm(false);
                          }
                      }
        );
        testScheduler.triggerActions();

        //when alarm fired
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        instance.snooze(newAlarm);
        testScheduler.triggerActions();

        AlarmStore record = containerFactory.getCreatedRecords().get(0);

        System.out.println("------------");
        // now we simulate it started all over again
        alarmSetterMock.removeRTCAlarm();

        final DatabaseQuery query = mock(DatabaseQuery.class);
        when(query.query()).thenReturn(Single.just(containerFactory.getCreatedRecords()));
        Alarms newAlarms = createAlarms(query);
        newAlarms.start();
        testScheduler.triggerActions();

        assertThat(alarmSetterMock.getId()).isEqualTo(record.getValue().getId());
    }

    @Test
    public void snoozedAlarmsMustCanBeRescheduled() {
        //given
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.edit(new Function1<AlarmValue, AlarmValue>() {
                          @Override
                          public AlarmValue invoke(AlarmValue alarmValue) {
                              return alarmValue
                                      .withIsEnabled(true)
                                      .withHour(7)
                                      .withDaysOfWeek(new DaysOfWeek(0))
                                      .withIsPrealarm(false);
                          }
                      }
        );
        testScheduler.triggerActions();

        //when alarm fired
        currentHour = 7;
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();

        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        instance.snooze(newAlarm);
        testScheduler.triggerActions();

        System.out.println("----- now snooze -------");

        newAlarm.snooze(7, 42);
        testScheduler.triggerActions();

        assertThat(alarmSetterMock.getId()).isEqualTo(newAlarm.getId());
        assertThat(alarmSetterMock.getCalendar().get(Calendar.MINUTE)).isEqualTo(42);
    }

    @Test
    public void snoozedAlarmsMustGoOutOfHibernationIfItWasRescheduled() {
        snoozedAlarmsMustCanBeRescheduled();

        AlarmStore record = containerFactory.getCreatedRecords().get(0);

        System.out.println("------------");
        // now we simulate it started all over again
        alarmSetterMock.removeRTCAlarm();

        final DatabaseQuery query = mock(DatabaseQuery.class);
        when(query.query()).thenReturn(Single.just(containerFactory.getCreatedRecords()));
        Alarms newAlarms = createAlarms(query);
        newAlarms.start();
        testScheduler.triggerActions();

        assertThat(alarmSetterMock.getId()).isEqualTo(record.getValue().getId());
        // TODO
        //  assertThat(alarmSetterMock.getCalendar().get(Calendar.MINUTE)).isEqualTo(42);
    }

    @Test
    public void prealarmFiredAlarmTransitioningToFiredShouldNotDismissTheService() {
        //given
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.edit(new Function1<AlarmValue, AlarmValue>() {
                          @Override
                          public AlarmValue invoke(AlarmValue alarmValue) {
                              return alarmValue.withIsEnabled(true)
                                      .withHour(0)
                                      .withDaysOfWeek(new DaysOfWeek(0))
                                      .withIsPrealarm(true);
                          }
                      }
        );
        testScheduler.triggerActions();

        //when alarm fired
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.PREALARM);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_PREALARM_ACTION));

        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));
        verify(stateNotifierMock, never()).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_DISMISS_ACTION));

        instance.snooze(newAlarm);
        testScheduler.triggerActions();
        verify(stateNotifierMock, times(1)).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_DISMISS_ACTION));
    }
}