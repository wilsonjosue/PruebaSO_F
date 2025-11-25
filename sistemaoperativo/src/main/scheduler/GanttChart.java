package scheduler;

import java.util.*;

/**
 * Genera y almacena el diagrama de Gantt de la ejecución
 * Muestra qué proceso ejecutó en cada momento
 */
public class GanttChart {
  private final List<GanttEntry> entries;
  private final List<String> events;
  private int currentTime;

  public GanttChart() {
    this.entries = new ArrayList<>();
    this.events = new ArrayList<>();
    this.currentTime = 0;
  }

  /**
   * Registra la ejecución de un proceso en el tiempo actual
   */
  public void recordExecution(String processId, int startTime) {
    // Completar entrada anterior si existe
    if (!entries.isEmpty()) {
      GanttEntry last = entries.get(entries.size() - 1);
      if (last.endTime == -1) {
        last.endTime = startTime;
      }
    }

    // Crear nueva entrada
    GanttEntry entry = new GanttEntry(processId, startTime, -1);
    entries.add(entry);
    currentTime = startTime;
  }
  
  /**
   * Agrega una ejecución con tiempo de inicio y fin
   */
  public void addExecution(String processId, int startTime, int endTime) {
    // Si el proceso anterior es el mismo y continúa, extender su tiempo
    if (!entries.isEmpty()) {
      GanttEntry last = entries.get(entries.size() - 1);
      if (last.processId.equals(processId) && last.endTime == startTime) {
        last.endTime = endTime;
        currentTime = endTime;
        return;
      }
    }
    
    GanttEntry entry = new GanttEntry(processId, startTime, endTime);
    entries.add(entry);
    currentTime = endTime;
  }
  
  /**
   * Agrega un evento al registro
   */
  public void addEvent(int time, String description) {
    events.add(String.format("[t=%d] %s", time, description));
  }

  /**
   * Finaliza el diagrama en el tiempo especificado
   */
  public void finalizeChart(int endTime) {
    if (!entries.isEmpty()) {
      GanttEntry last = entries.get(entries.size() - 1);
      if (last.endTime == -1) {
        last.endTime = endTime;
      }
    }
  }

  /**
   * Representación del diagrama de Gantt
   */
  @Override
  public String toString() {
    if (entries.isEmpty()) {
      return "Diagrama de Gantt vacío";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("\n=== DIAGRAMA DE GANTT ===\n");

    // Consolidar entradas consecutivas del mismo proceso
    List<GanttEntry> consolidated = consolidateEntries();
    
    // Línea de procesos
    sb.append("Proceso: ");
    for (GanttEntry entry : consolidated) {
      int duration = entry.endTime - entry.startTime;
      String pad = repeatChar('-', Math.max(1, duration - 1));
      sb.append(String.format("| %s%s ", entry.processId, pad));
    }
    sb.append("|\n");

    // Línea de tiempos
    sb.append("Tiempo:  ");
    for (GanttEntry entry : consolidated) {
      int duration = entry.endTime - entry.startTime;
      String spaces = repeatChar(' ', Math.max(0, duration - String.valueOf(entry.startTime).length()));
      sb.append(String.format("%d%s ", entry.startTime, spaces));
    }
    if (!consolidated.isEmpty()) {
      GanttEntry last = consolidated.get(consolidated.size() - 1);
      sb.append(String.format("%d", last.endTime));
    }
    sb.append("\n");
    
    // Eventos importantes
    if (!events.isEmpty()) {
      sb.append("\n=== EVENTOS ===\n");
      for (String event : events) {
        sb.append(event).append("\n");
      }
    }

    return sb.toString();
  }
  
  /**
   * Consolida entradas consecutivas del mismo proceso
   */
  private List<GanttEntry> consolidateEntries() {
    if (entries.isEmpty()) {
      return new ArrayList<>();
    }
    
    List<GanttEntry> consolidated = new ArrayList<>();
    GanttEntry current = entries.get(0);
    
    for (int i = 1; i < entries.size(); i++) {
      GanttEntry next = entries.get(i);
      
      if (current.processId.equals(next.processId) && current.endTime == next.startTime) {
        // Extender la entrada actual
        current.endTime = next.endTime;
      } else {
        // Agregar la entrada actual y comenzar una nueva
        consolidated.add(current);
        current = next;
      }
    }
    
    // Agregar la última entrada
    consolidated.add(current);
    
    return consolidated;
  }
  
  /**
   * Repite un carácter n veces
   */
  private String repeatChar(char c, int times) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < times; i++) {
      sb.append(c);
    }
    return sb.toString();
  }

  /**
   * Entrada individual del diagrama de Gantt
   */
  private static class GanttEntry {
    String processId;
    int startTime;
    int endTime;

    GanttEntry(String processId, int startTime, int endTime) {
      this.processId = processId;
      this.startTime = startTime;
      this.endTime = endTime;
    }
  }
}