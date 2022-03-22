package nessusTools.sync;

import net.bytebuddy.implementation.bytecode.*;

import java.util.*;

/**
 * Simple class for synchronizing concurrent read access and exclusive write access to an object.
 *
 * Object type = O
 * Return value type = R
 *
 * Multiple threads can read at one time.  Only one thread can write at a time, and no other
 * threads can read while the write thread holds the lock.  Note that the write thread will still be
 * able to run read operations with the read(runnable) method while holding the write lock.
 * Also note that a read thread must release its read lock BEFORE it requests a write lock, otherwise
 * deadlock will occur.
 *
 * Type parameter R is the type of the return value from the CallableWithArg.  If no return
 * is needed or used, just return null and ignore the return value;
 *
 * Type parameter E is the type of any throwable which the runnable might throw.  If no
 * exceptions will be thrown, then use the static inner class NothingThrown as the type for E.
 * NothingThrown cannot be instantiated, and because it extends RuntimeException it does not
 * need to be caught or declared.
 *
 */
public class ReadWriteLock<O, R, E extends Throwable> {
    private final List<Lock> readLocks = new LinkedList<>();
    private final Lock addLock = new Lock();
    private final Lock removeLock = new Lock();
    private final O object;
    private Thread currentWriteLock = null;
    private int writeLockCounter = 0;

    public ReadWriteLock(O objectToLock) {
        this.object = objectToLock;
    }

    private class Lock {
        private Thread thread = Thread.currentThread();
        private boolean active = true;
        public boolean equals(Object o) {
            return o == this;
        }
    }

    public final R read(CallableWithArg<O, R, E> runnable) throws E {
        Lock readLock = new Lock();
        R returnVal = null;
        synchronized (readLock) {
            try {
                synchronized (addLock) {
                    synchronized (removeLock) {
                        readLocks.add(readLock);
                    }
                }

                returnVal = runnable.run(object);

            } finally {
                readLock.active = false;
                // don't hold up the current thread with removing the read lock from the list
                (new Thread(() -> {
                    synchronized (removeLock) {
                        readLocks.remove(readLock);
                    }
                })).start();
            }
        }

        return returnVal;
    }

    public final R write(CallableWithArg<O, R, E> runnable) throws E {
        synchronized (addLock) {
            this.writeLockCounter++;
            if (this.currentWriteLock == null) {
                // skip this check when there was already a writeLock.  It is probably this thread
                this.currentWriteLock = Thread.currentThread();

                int size;
                while ((size = readLocks.size()) > 0) {
                    try {
                        Lock lock;
                        synchronized (lock = readLocks.get(size - 1)) {
                            if (lock.active && Objects.equals(lock.thread, this.currentWriteLock)) {
                                throw new IllegalAccessError("A thread cannot grab a write lock until it has released its read lock!");
                            }

                            boolean allInactive = true;
                            synchronized (removeLock) {
                                for (Lock l : this.readLocks) {
                                    if (lock.active) {
                                        allInactive = false;
                                        break;
                                    }
                                }
                            }
                            if (allInactive) {
                                break;
                            }
                        }
                    } catch (IndexOutOfBoundsException e) {
                        //  ^^^ in case of race conditions, where the size of the list changed
                        // between size() and get(size - 1)
                    }
                }
            } else if (!Objects.equals(this.currentWriteLock, Thread.currentThread())) {
                throw new IllegalAccessError("Unexpected thread in the currentWriteLock");
            }

            R returnVal = runnable.run(this.object);

            this.writeLockCounter--;
            if (this.writeLockCounter <= 0) {
                this.writeLockCounter = 0;
                this.currentWriteLock = null;
            }

            return returnVal;
        }
    }
}
