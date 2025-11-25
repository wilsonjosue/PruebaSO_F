package scheduler;

/**
 * Reloj global sincronizado para la simulacion
 * Mantiene consistencia en el tiempo entre todos los componentes
 */
public class SimulationClock {
  private static int currentTime = 0;
  private static final Object lock = new Object();

  public static int getTime() {
    synchronized (lock) {
      return currentTime;
    }
  }

  public static void incrementTime() {
    synchronized (lock) {
      currentTime++;
    }
  }

  public static void setTime(int time) {
    synchronized (lock) {
      currentTime = time;
    }
  }

  public static void reset() {
    synchronized (lock) {
      currentTime = 0;
    }
  }
}