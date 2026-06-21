package ru.cubeshield.cubecore.modules

import kotlinx.coroutines.*
import net.fabricmc.fabric.api.event.player.BlockEvents
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.InteractionResult
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.api.ApiResponse
import ru.cubeshield.cubecore.api.dto.BillCreateDto
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import ru.cubeshield.cubecore.player.PlayerCache
import ru.cubeshield.cubecore.utils.MessageUtil
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

class SignPaymentModule : ICubeModule {
    override val id = "sign_payment_module"
    override val name = "Sign Payment Module"
    override val description = "Модуль, отвечающий за оплату по табличке"

    private val paymentPattern = Regex("@(\\w+)>(\\d+)АР")

    private val playerCooldowns = ConcurrentHashMap<String, Long>()
    private val COOLDOWN_DURATION_MS = 1500L

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        BlockEvents.USE_WITHOUT_ITEM.register { state, world, blockPos, player, hitResult ->
            if (world.isClientSide) {
                return@register InteractionResult.PASS
            }

            val currentTime = System.currentTimeMillis()
            val lastClickTime = playerCooldowns[player.gameProfile.name]

            if (lastClickTime != null && (currentTime - lastClickTime) < COOLDOWN_DURATION_MS) {
                MessageUtil.send(player, "Пожалуйста подождите...", false, false)
                return@register InteractionResult.SUCCESS
            }

            val toPlayerId = PlayerCache.getId(player.gameProfile.name) ?: return@register InteractionResult.PASS
            val blockEntity = world.getBlockEntity(hitResult.blockPos)

            if (blockEntity is SignBlockEntity) {
                val signText = blockEntity.getFrontText().getMessages(false)
                    .joinToString(separator = " ") { it.string }
                val matches = paymentPattern.findAll(signText)
                if (matches.any()) {
                    matches.forEach { matchResult ->
                        val (playername, amountString) = matchResult.destructured
                        if (player.gameProfile.name == playername) return@register InteractionResult.PASS
                        val amount = amountString.toIntOrNull() ?: return@register InteractionResult.PASS
                        playerCooldowns[player.gameProfile.name] = currentTime
                        modScope.launch {
                            var fromPlayerId: String? = null
                            when (val result = apiClient.getPlayer(playername)) {
                                is ApiResponse.Success -> {
                                    fromPlayerId = result.data.id
                                }
                                is ApiResponse.Error -> {
                                    MessageUtil.send(player, "Игрока с таким ником не существует", true, false)
                                }
                            }
                            if (fromPlayerId == null) return@launch
                            MessageUtil.send(player, "Обработка платежа...", false, false)
                            when (val result = apiClient.createAutoPayBill(fromPlayerId, BillCreateDto(
                                toPlayerId = toPlayerId,
                                amount = amount,
                                note = "Система Берестовых Платежей",
                                until = kotlin.time.Clock.System.now() + 10.minutes
                            ))) {
                                is ApiResponse.Success -> {}
                                is ApiResponse.Error -> {
                                    MessageUtil.send(player, "Недостаточно средств", true, false)
                                }
                            }
                        }
                        return@register InteractionResult.SUCCESS
                    }
                    return@register InteractionResult.SUCCESS
                }
            }
            InteractionResult.PASS
        }
    }

    override fun shutdown() {}
}