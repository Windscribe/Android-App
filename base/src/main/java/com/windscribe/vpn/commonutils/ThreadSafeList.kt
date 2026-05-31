package com.windscribe.vpn.commonutils

class ThreadSafeList<T> : MutableList<T> {
    private val list = mutableListOf<T>()
    private val lock = Any()

    override val size: Int
        get() = synchronized(lock) { list.size }

    override fun contains(element: T): Boolean = synchronized(lock) { list.contains(element) }

    override fun containsAll(elements: Collection<T>): Boolean = synchronized(lock) { list.containsAll(elements) }

    override fun get(index: Int): T = synchronized(lock) { list[index] }

    override fun indexOf(element: T): Int = synchronized(lock) { list.indexOf(element) }

    override fun isEmpty(): Boolean = synchronized(lock) { list.isEmpty() }

    override fun iterator(): MutableIterator<T> = synchronized(lock) { list.toMutableList().iterator() }

    override fun lastIndexOf(element: T): Int = synchronized(lock) { list.lastIndexOf(element) }

    override fun add(
        index: Int,
        element: T,
    ) {
        synchronized(lock) { list.add(index, element) }
    }

    override fun addAll(
        index: Int,
        elements: Collection<T>,
    ): Boolean = synchronized(lock) { list.addAll(index, elements) }

    override fun addAll(elements: Collection<T>): Boolean = synchronized(lock) { list.addAll(elements) }

    override fun clear() {
        synchronized(lock) { list.clear() }
    }

    override fun listIterator(): MutableListIterator<T> = synchronized(lock) { list.toMutableList().listIterator() }

    override fun listIterator(index: Int): MutableListIterator<T> = synchronized(lock) { list.toMutableList().listIterator(index) }

    override fun removeAll(elements: Collection<T>): Boolean = synchronized(lock) { list.removeAll(elements) }

    override fun removeAt(index: Int): T = synchronized(lock) { list.removeAt(index) }

    override fun retainAll(elements: Collection<T>): Boolean = synchronized(lock) { list.retainAll(elements) }

    override fun set(
        index: Int,
        element: T,
    ): T = synchronized(lock) { list.set(index, element) }

    override fun subList(
        fromIndex: Int,
        toIndex: Int,
    ): MutableList<T> = synchronized(lock) { list.subList(fromIndex, toIndex) }

    override fun add(element: T): Boolean = synchronized(lock) { list.add(element) }

    override fun remove(element: T): Boolean = synchronized(lock) { list.remove(element) }

    override fun toString(): String = synchronized(lock) { list.toString() }
}
