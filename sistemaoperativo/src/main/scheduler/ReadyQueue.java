package scheduler;

import model.Process;
import java.util.*;
import java.util.concurrent.locks.*;

/**
 * Cola de procesos lista con sincronización para acceso concurrente
 * Evita condiciones de carrera cuando varios hilos acceden
 */
public class ReadyQueue {
  private final Queue<Process> queue;
  private final Lock lock;
  private final Condition notEmpty;

  public ReadyQueue() {
    this.queue = new LinkedList<>();
    this.lock = new ReentrantLock();
    this.notEmpty = lock.newCondition();
  }

  /**
   * Agrega un proceso a la cola de manera segura
   */
  public void addProcess(Process process) {
    lock.lock();
    try {
      queue.offer(process);
      notEmpty.signalAll(); // Notificar a hilos esperando
      System.out.println("ReadyQueue: Proceso " + process.getPid() +
          " agregado. Tamaño: " + queue.size());
    } finally {
      lock.unlock();
    }
  }

  /**
   * Obtiene y remueve el primer proceso de la cola
   * Si la cola está vacía, bloquea hasta que haya procesos
   */
  public Process getNextProcess() throws InterruptedException {
    lock.lock();
    try {
      while (queue.isEmpty()) {
        System.out.println("ReadyQueue: Cola vacía, esperando...");
        notEmpty.await(); // Esperar hasta que haya procesos
      }

      Process process = queue.poll();
      System.out.println("ReadyQueue: Proceso " + process.getPid() +
          " removido. Tamaño: " + queue.size());
      return process;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Obtiene el primer proceso sin removerlo
   */
  public Process peekNextProcess() {
    lock.lock();
    try {
      return queue.peek();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Obtiene todos los procesos en la cola (para visualizarlos)
   */
  public List<Process> getAllProcesses() {
    lock.lock();
    try {
      return new ArrayList<>(queue);
    } finally {
      lock.unlock();
    }
  }

  public int size() {
    lock.lock();
    try {
      return queue.size();
    } finally {
      lock.unlock();
    }
  }

  public boolean isEmpty() {
    lock.lock();
    try {
      return queue.isEmpty();
    } finally {
      lock.unlock();
    }
  }
}