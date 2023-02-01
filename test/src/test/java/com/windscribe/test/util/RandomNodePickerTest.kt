package com.windscribe.test.util

import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.serverlist.entity.Node
import org.junit.Test
import kotlin.random.Random

class RandomNodePickerTest {

    @Test
    fun testRandomNodePicker() {
        val listOfNodes = mutableListOf<Node>()
        for (i in 0..Random.nextInt(10)) {
            val node = Node()
            node.hostname = "$i"
            node.weight = Random.nextInt(5)
            listOfNodes.add(node)
        }
        val lastUsed = Random.nextInt(listOfNodes.size - 1)
        val attempt = Random.nextInt(1)
        val chosenIndex = Util.getRandomNode(lastUsed, attempt, listOfNodes)
        assert(chosenIndex != lastUsed)
        assert(chosenIndex < listOfNodes.size)
    }
}