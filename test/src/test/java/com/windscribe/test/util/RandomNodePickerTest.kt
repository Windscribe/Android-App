package com.windscribe.test.util

import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.serverlist.entity.Server
import org.junit.Test
import kotlin.random.Random

class RandomNodePickerTest {

    @Test
    fun testRandomNodePicker() {
        val listOfServers = mutableListOf<Server>()
        for (i in 0..Random.nextInt(10)) {
            val server = Server(
                id = i,
                hostname = "$i",
                ip = "127.0.0.$i",
                ip2 = "127.0.0.$i",
                ip3 = "127.0.0.$i",
                datacenterId = 1,
                weight = Random.nextInt(5),
                health = 100
            )
            listOfServers.add(server)
        }
        val lastUsed = Random.nextInt(listOfServers.size - 1)
        val attempt = Random.nextInt(1)
        val chosenIndex = Util.getRandomNode(lastUsed, attempt, listOfServers)
        assert(chosenIndex != lastUsed)
        assert(chosenIndex < listOfServers.size)
    }
}