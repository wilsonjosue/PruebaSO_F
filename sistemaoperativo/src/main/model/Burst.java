//src/main/model/Burst.java
package model;

/**
 * Representa una rafaga de CPU o E/S en un proceso
 */
public class Burst {
  private BurstType type;
  private int duration;
  private int remainingTime;

  public enum BurstType {
    CPU, IO
  }

  public Burst(BurstType type, int duration) {
    this.type = type;
    this.duration = duration;
    this.remainingTime = duration;
  }

  // Getters y Setters
  public BurstType getType() {
    return type;
  }

  public int getDuration() {
    return duration;
  }

  public int getRemainingTime() {
    return remainingTime;
  }

  public void setRemainingTime(int remainingTime) {
    this.remainingTime = remainingTime;
  }

  /**
   * Verifica si la rafaga ha sido completada
   */
  public boolean isCompleted() {
    return remainingTime <= 0;
  }

  @Override
  public String toString() {
    return String.format("%s(%d)", type, duration);
  }
}