# Cron Task Scheduler

Cron parser taken from https://github.com/frode-carlsen/cron and package names for classes taken from there are left the same so we can easily absorb them.

An fork we may want to absorb is https://github.com/zemiak/cron that uses the Time API in Java 8 instead of Joda.

## Example usage

```
public static void main(String[] args) throws Exception {
  TaskManager taskExecutor = new TaskManager();
  taskExecutor.executeTaskOnSchedule(new Task() {
    int count = 0;
    @Override
    public String getId() {
      return "executor";
    }

    @Override
    public void execute() {
      count++;
      System.out.println("Executing run " + count);
    }
  }, "0/10 * * * * *");
  Thread.sleep(30000);
  taskExecutor.stop();
}
```