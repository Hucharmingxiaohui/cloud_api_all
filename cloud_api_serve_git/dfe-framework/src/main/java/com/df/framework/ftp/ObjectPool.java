package com.df.framework.ftp;

import java.util.Enumeration;
import java.util.Hashtable;

abstract class ObjectPool<T> {

    long ttl;
    Hashtable<T, Long> lock, unlock;

    public ObjectPool() {
        ttl = 50000;
        lock = new Hashtable<T, Long>();
        unlock = new Hashtable<T, Long>();
    }

    abstract T create();

    abstract boolean valide(T t);

    abstract void destory(T t);

    public synchronized T takeOut() {
        long now = SystemClock.now();
        T t = null;
        if (unlock.size() > 0) {
            Enumeration<T> e = unlock.keys();
            while (e.hasMoreElements()) {
                t = e.nextElement();
                if ((now - unlock.get(t) > ttl)) {
                    unlock.remove(t);
                    destory(t);
                    t = null;
                } else if (!valide(t)) {
                    unlock.remove(t);
                    lock.put(t, SystemClock.now());
                    System.out.println("从FTPS连接池中取出一个连接============>");
                    return t;
                }
            }
        }
        t = create();
        if (t != null) {
            lock.put(t, SystemClock.now());
        }
        return t;
    }

    public synchronized void takeIn(T t) {
        lock.remove(t);
        unlock.put(t, SystemClock.now());
    }

    public synchronized void remove(T t) {
        lock.remove(t);
    }
}
