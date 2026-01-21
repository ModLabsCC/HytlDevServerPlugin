package cc.modlabs

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit

class HytlDev(init: JavaPluginInit) : JavaPlugin(init) {

    lateinit var instance: HytlDev

    override fun setup() {
        instance = this

    }

}