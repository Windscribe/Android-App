package com.windscribe.vpn.commonutils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ThreadSafeListTest {

    @Test
    fun `new list is empty`() {
        val list = ThreadSafeList<String>()

        assertEquals(0, list.size)
        assertTrue(list.isEmpty())
    }

    @Test
    fun `add and get preserve insertion order`() {
        val list = ThreadSafeList<String>()
        list.add("a")
        list.add("b")
        list.add("c")

        assertEquals(3, list.size)
        assertEquals("a", list[0])
        assertEquals("b", list[1])
        assertEquals("c", list[2])
    }

    @Test
    fun `add at index inserts at correct position`() {
        val list = ThreadSafeList<Int>()
        list.add(1)
        list.add(3)
        list.add(1, 2)

        assertEquals(listOf(1, 2, 3), list.toList())
    }

    @Test
    fun `remove by element returns true and shrinks list`() {
        val list = ThreadSafeList<String>()
        list.add("a")
        list.add("b")

        assertTrue(list.remove("a"))
        assertEquals(1, list.size)
        assertEquals("b", list[0])
    }

    @Test
    fun `remove of absent element returns false`() {
        val list = ThreadSafeList<String>()
        list.add("a")

        assertFalse(list.remove("missing"))
        assertEquals(1, list.size)
    }

    @Test
    fun `removeAt returns removed value`() {
        val list = ThreadSafeList<String>()
        list.add("a")
        list.add("b")
        list.add("c")

        val removed = list.removeAt(1)

        assertEquals("b", removed)
        assertEquals(listOf("a", "c"), list.toList())
    }

    @Test
    fun `indexOf and lastIndexOf find correct positions`() {
        val list = ThreadSafeList<String>()
        list.addAll(listOf("a", "b", "a", "c"))

        assertEquals(0, list.indexOf("a"))
        assertEquals(2, list.lastIndexOf("a"))
        assertEquals(-1, list.indexOf("missing"))
    }

    @Test
    fun `contains and containsAll`() {
        val list = ThreadSafeList<Int>()
        list.addAll(listOf(1, 2, 3))

        assertTrue(list.contains(2))
        assertFalse(list.contains(99))
        assertTrue(list.containsAll(listOf(1, 3)))
        assertFalse(list.containsAll(listOf(1, 99)))
    }

    @Test
    fun `clear empties the list`() {
        val list = ThreadSafeList<Int>()
        list.addAll(listOf(1, 2, 3))

        list.clear()

        assertTrue(list.isEmpty())
        assertEquals(0, list.size)
    }

    @Test
    fun `set replaces and returns old value`() {
        val list = ThreadSafeList<String>()
        list.add("a")
        list.add("b")

        val old = list.set(0, "z")

        assertEquals("a", old)
        assertEquals("z", list[0])
    }

    @Test
    fun `iterator snapshots the list and does not see later mutations`() {
        // ThreadSafeList.iterator() returns an iterator over a copy — that's the
        // contract that makes concurrent iteration safe. Verify the snapshot
        // behaviour so future changes don't silently regress it.
        val list = ThreadSafeList<Int>()
        list.addAll(listOf(1, 2, 3))

        val iterator = list.iterator()
        list.add(4)

        val seen = mutableListOf<Int>()
        while (iterator.hasNext()) seen.add(iterator.next())

        assertEquals(listOf(1, 2, 3), seen)
        assertEquals(4, list.size)
    }

    @Test
    fun `concurrent adds from many threads do not lose elements`() {
        val list = ThreadSafeList<Int>()
        val threadCount = 16
        val perThread = 500
        val executor = Executors.newFixedThreadPool(threadCount)

        try {
            repeat(threadCount) { t ->
                executor.submit {
                    repeat(perThread) { i -> list.add(t * perThread + i) }
                }
            }
        } finally {
            executor.shutdown()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        }

        assertEquals(threadCount * perThread, list.size)
        assertEquals(threadCount * perThread, list.toSet().size)
    }
}