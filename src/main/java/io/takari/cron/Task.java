package io.takari.cron;

public interface Task {
  void execute();
  String getId();  
}
