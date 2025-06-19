package ru.cubeshield.cubecore.modules


import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.entity.SignBlockEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.api.ApiResponse
import ru.cubeshield.cubecore.api.dto.BillCreateDto
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.utils.MessageUtil
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


class SignPaymentModule : ICubeModule {
    override val id = "sign_payment_module"
    override val name = "Sign Payment Module"
    override val description = "Модуль, отвечающий за оплату по табличке"

    private val paymentPattern = Regex("@(\\w+)>(\\d+)АР")

    private var cachedPlayersIds = ConcurrentHashMap<String, String>()

    private val playerCooldowns = ConcurrentHashMap<String, Long>()
    private val COOLDOWN_DURATION_MS = 1500L

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerAuthorized> { (player, playerId, _, _) ->
            cachedPlayersIds[player.gameProfile.name] = playerId
        }

        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            if (world.isClient) {
                return@register ActionResult.PASS
            }

            val currentTime = System.currentTimeMillis()
            val lastClickTime = playerCooldowns[player.gameProfile.name]

            if (lastClickTime != null && (currentTime - lastClickTime) < COOLDOWN_DURATION_MS) {
                MessageUtil.send(player, "Пожалуйста подождите...")
                return@register ActionResult.SUCCESS
            }

            val toPlayerId = cachedPlayersIds[player.gameProfile.name] ?: return@register ActionResult.PASS
            val blockEntity = world.getBlockEntity(hitResult.blockPos)

            if (blockEntity is SignBlockEntity) {
                val signText = blockEntity.getText(true).getMessages(true).joinToString(separator = " ") { it.string }
                val matches = paymentPattern.findAll(signText)
                if (matches.any()) {
                    matches.forEach { matchResult ->
                        val (playername, amountString) = matchResult.destructured
                        if (player.gameProfile.name == playername) return@register ActionResult.PASS
                        val amount = amountString.toIntOrNull() ?: return@register ActionResult.PASS
                        playerCooldowns[player.gameProfile.name] = currentTime
                        modScope.launch {
                            var fromPlayerId: String? = null
                            when (val result = apiClient.getPlayer(playername)) {
                                is ApiResponse.Success -> {
                                    fromPlayerId = result.data.id
                                }
                                is ApiResponse.Error -> {
                                    MessageUtil.send(player, "Игрока с таким ником не существует")
                                }
                            }
                            if (fromPlayerId == null ) return@launch
                            MessageUtil.send(player, "Обработка платежа...")
                            when (val result = apiClient.createAutoPayBill(fromPlayerId, BillCreateDto(
                                toPlayerId = toPlayerId,
                                amount = amount,
                                note = "Система Берестовых Платежей",
                                until = Clock.System.now() + 10.minutes
                            ))) {
                                is ApiResponse.Success -> {}
                                is ApiResponse.Error -> {
                                    MessageUtil.send(player, "Недостаточно средств")
                                }
                            }
                        }
                        return@register ActionResult.SUCCESS
                    }
                    return@register ActionResult.SUCCESS
                }
            }
            ActionResult.PASS
        }
    }


    override fun shutdown() {}
}