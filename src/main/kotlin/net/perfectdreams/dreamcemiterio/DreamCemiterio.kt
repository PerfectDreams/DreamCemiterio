package net.perfectdreams.dreamcemiterio

import com.github.salomonbrys.kotson.fromJson
import net.perfectdreams.dreamcore.utils.DreamUtils
import net.perfectdreams.dreamcore.utils.KotlinPlugin
import net.perfectdreams.dreamcore.utils.commands.AbstractCommand
import net.perfectdreams.dreamcore.utils.commands.annotation.Subcommand
import net.perfectdreams.dreamcore.utils.registerEvents
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.Skull
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import java.io.File
import java.util.*

class DreamCemiterio : KotlinPlugin(), Listener {
	var graves = mutableListOf<Grave>()
	var graveIdx = 0
	val axis = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST) // arrayOf(BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH)

	fun yawToFace(yaw: Float): BlockFace {
		val axis = arrayOf(BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST)
		Bukkit.broadcastMessage(axis[Math.round(yaw / 90f) and 0x3].toString())
		return axis[Math.round(yaw / 90f) and 0x3]
	}

	override fun softEnable() {
		super.softEnable()

		dataFolder.mkdirs()

		val saveFile = File(dataFolder, "graves.json")
		if (saveFile.exists()) {
			graves = DreamUtils.gson.fromJson(saveFile.readText())
		}

		registerEvents(this)
		registerCommand(object: AbstractCommand("dreamcemiterio", permission = "dreamcemiterio.setup") {
			@Subcommand
			fun onCommand(p0: CommandSender) {
				p0.sendMessage("/dreamcemiterio add")
				p0.sendMessage("/dreamcemiterio remove idx")
				p0.sendMessage("/dreamcemiterio check")
			}

			@Subcommand(["add"])
			fun add(player: Player) {
				val grave = Grave(player.getTargetBlock(null as Set<Material>?, 10).location, yawToFace(player.location.yaw))
				graves.add(grave)
				player.sendMessage("§aMarcado com sucesso!")

				saveFile.writeText(DreamUtils.gson.toJson(graves))
			}

			@Subcommand(["remove"])
			fun remove(player: Player, idx: Int) {
				graves.removeAt(idx.toInt())
				player.sendMessage("§aRemovido!")
			}

			@Subcommand(["check"])
			fun check(player: Player) {
				val color = ChatColor.values()[DreamUtils.random.nextInt(ChatColor.values().size)]

				val toRemove = mutableListOf<Grave>()
				for ((index, grave) in graves.withIndex()) {
					val block = grave.location.block
					if (block.type != Material.WALL_SIGN) {
						player.sendMessage("$index. §c${grave.location.x}, ${grave.location.y}, ${grave.location.z} ~ faltando placa!")
						toRemove.add(grave)
						continue
					}

					val sign = block.state as Sign
					sign.setLine(0, color.toString() + "DEU RUIM!!!")
					sign.setLine(1, color.toString() + index)
					sign.setLine(2, "")
					sign.setLine(3, "me corrige :3")

					if (block.getRelative(BlockFace.UP).getRelative(grave.face).type != Material.PLAYER_HEAD) {
						player.sendMessage("$index. §c${grave.location.x}, ${grave.location.y}, ${grave.location.z} ~ cabeça faltando!")
						toRemove.add(grave)
						continue
					}

					sign.setLine(0, color.toString() + grave.face.name)
					sign.setLine(1, color.toString() + index)
					sign.setLine(2, "")
					sign.setLine(3, "skull? ${block.getRelative(BlockFace.UP).getRelative(grave.face).type == Material.PLAYER_HEAD}")
					sign.update()
				}
				graves.removeAll(toRemove)
				saveFile.writeText(DreamUtils.gson.toJson(graves))
			}
		})
	}

	override fun softDisable() {
		super.softDisable()
	}

	@EventHandler
	fun onDeath(e: PlayerDeathEvent) {
		var grave = graves.getOrNull(graveIdx)

		if (grave == null) {
			graveIdx = 0
			grave = graves.getOrNull(graveIdx)

			if (grave == null)
				return
		}

		val block = grave.location.block
		val sign = block.state as Sign
		sign.setLine(0, "§4§lDescanse em")
		sign.setLine(1, "§4§lPaz...")

		sign.setLine(2, e.entity.name)
		val calendar = Calendar.getInstance()
		val day = calendar[Calendar.DAY_OF_MONTH].toString().padStart(2, '0')
		val month = (calendar[Calendar.MONTH] + 1).toString().padStart(2, '0')
		val year = calendar[Calendar.YEAR]
		val hour = calendar[Calendar.HOUR_OF_DAY].toString().padStart(2, '0')
		val minute = (calendar[Calendar.MINUTE] + 1).toString().padStart(2, '0')

		sign.setLine(3, "$day/$month/$year $hour:$minute")
		sign.update()

		val skull = block.getRelative(BlockFace.UP).getRelative(grave.face).state as Skull
		skull.owningPlayer = e.entity
		skull.update()

		grave.location.world.spawnParticle(Particle.VILLAGER_ANGRY, grave.location, 80, 0.5, 0.5, 0.5)
		graveIdx++
	}

	class Grave(
			val location: Location,
			val face: BlockFace
	)
}