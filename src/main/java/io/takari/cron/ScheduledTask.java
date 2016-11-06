package io.takari.cron;

import fc.cron.CronExpression;

public class ScheduledTask {

  private final Task task;
  private final CronExpression schedule;
  
  public ScheduledTask(Task task, CronExpression schedule) {
    this.task = task;
    this.schedule = schedule;
  }

  public Task getTask() {
    return task;
  }

  public CronExpression getSchedule() {
    return schedule;
  }
}
