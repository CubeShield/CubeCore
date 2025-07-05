package ru.cubeshield.cubecore.modules

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

import kotlinx.coroutines.*
import net.minecraft.server.network.ServerPlayerEntity
import ru.cubeshield.cubecore.api.ApiClient
import ru.cubeshield.cubecore.config.ModConfig
import ru.cubeshield.cubecore.event.*
import java.util.concurrent.ConcurrentHashMap

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import ru.cubeshield.cubecore.api.ApiResponse
import ru.cubeshield.cubecore.utils.MessageUtil
import kotlin.math.max
import kotlin.math.min


class BankModule : ICubeModule {
    override val id = "bank_module"
    override val name = "Bank Module"
    override val description = "Модуль, отвечающий за банкинг"

    private var cachedPlayersIds = ConcurrentHashMap<String, String>()

    override fun initialize(eventBus: EventBus, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        eventBus.subscribe<PlayerAuthorized> { (player, playerId, _, _) ->
            cachedPlayersIds[player.gameProfile.name] = playerId
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCommands(dispatcher, apiClient, config, modScope)
        }
    }

    private fun isPlayerInBankArea(player: ServerPlayerEntity, config: ModConfig): Boolean {
        val corner1 = BlockPos(
            config.modules.bankModule.fromX,
            config.modules.bankModule.fromY,
            config.modules.bankModule.fromZ,
        )
        val corner2 = BlockPos(
            config.modules.bankModule.toX,
            config.modules.bankModule.toY,
            config.modules.bankModule.toZ,
        )

        val playerPos = player.blockPos


        return playerPos.x >= min(corner1.x, corner2.x) && playerPos.x <= max(corner1.x, corner2.x) &&
                playerPos.y >= min(corner1.y, corner2.y) && playerPos.y <= max(corner1.y, corner2.y) &&
                playerPos.z >= min(corner1.z, corner2.z) && playerPos.z <= max(corner1.z, corner2.z)
    }

    private fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>, apiClient: ApiClient, config: ModConfig, modScope: CoroutineScope) {
        dispatcher.register(
            CommandManager.literal("balance")
                .executes { context ->
                    if (context.source.player == null) return@executes 1
                    val sender = context.source.playerOrThrow
                    modScope.launch {
                        val playerId = cachedPlayersIds[sender.gameProfile.name] ?: return@launch
                        when (val result = apiClient.getPlayerProfile(playerId)) {
                            is ApiResponse.Success -> {
                                var message = "Ваш баланс: ${result.data.balance} АР"
                                if (result.data.governmentBalance != null) {
                                    message += " • Государственная казна: ${result.data.governmentBalance} АР"
                                }
                                MessageUtil.send(sender, message, false)
                            }
                            is ApiResponse.Error -> {
                                logger.error("aob", result.exception)
                                MessageUtil.send(sender, "Не удалось получить ваш баланс", true)
                            }
                        }


                    }
                    1
                }
        )
        dispatcher.register(
            CommandManager.literal("bank")
                .executes { context ->
                    context.source.player?.let { MessageUtil.send(it, "Использование: /bank <deposit (пополнение) | withdraw (снятие)> <сумма>", true, false) }
                    1
                }
                .then(
                    CommandManager.literal("deposit")
                        .then(
                            CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                .executes { context ->
                                    if (context.source.player == null) return@executes 1
                                    if (!isPlayerInBankArea(context.source.player!!, config)) {
                                        MessageUtil.send(context.source.player!!, "Использование: /bank <deposit (пополнение) | withdraw (снятие)> <сумма>", true, false)
                                        return@executes 1
                                    }
                                    val sender = context.source.playerOrThrow
                                    val amount = IntegerArgumentType.getInteger(context, "amount")
                                    handleDeposit(sender, amount, apiClient, modScope)
                                    1
                                }
                        )
                )
                .then(
                    CommandManager.literal("withdraw")
                        .then(
                            CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                .executes { context ->
                                    if (context.source.player == null) return@executes 1
                                    if (!isPlayerInBankArea(context.source.player!!, config)) {
                                        MessageUtil.send(context.source.player!!, "Использование: /bank <deposit (пополнение) | withdraw (снятие)> <сумма>", true, false)
                                        return@executes 1
                                    }
                                    val sender = context.source.playerOrThrow
                                    val amount = IntegerArgumentType.getInteger(context, "amount")
                                    handleWithdraw(sender, amount, apiClient, modScope)
                                    1
                                }
                        )
                )
        )
    }

    private fun handleDeposit(player: ServerPlayerEntity, amount: Int, apiClient: ApiClient, modScope: CoroutineScope) {
        val playerId = cachedPlayersIds[player.gameProfile.name] ?: return
        val totalOre = player.inventory.count(Items.DIAMOND_ORE) + player.inventory.count(Items.DEEPSLATE_DIAMOND_ORE)

        if (totalOre < amount) {
            MessageUtil.send(player, "У вас недостаточно алмазной руды. Требуется: $amount, найдено: $totalOre", true)
            return
        }

        var remainingToRemove = amount
        for (i in 0 until player.inventory.size()) {
            if (remainingToRemove <= 0) break

            val stack = player.inventory.getStack(i)
            if (stack.isOf(Items.DIAMOND_ORE) || stack.isOf(Items.DEEPSLATE_DIAMOND_ORE)) {
                val toRemoveFromStack = min(remainingToRemove, stack.count)
                stack.decrement(toRemoveFromStack)
                remainingToRemove -= toRemoveFromStack
            }
        }


        modScope.launch {
            apiClient.createPlayerBankTransaction(playerId, amount)
        }
    }

    private fun handleWithdraw(player: ServerPlayerEntity, amount: Int, apiClient: ApiClient, modScope: CoroutineScope) {
        val playerId = cachedPlayersIds[player.gameProfile.name] ?: return
        modScope.launch {
            when (val result = apiClient.createPlayerBankTransaction(playerId, -amount)) {
                is ApiResponse.Success -> {
                    player.server?.execute {
                        val itemStack = ItemStack(Items.DEEPSLATE_DIAMOND_ORE, amount)
                        player.inventory.offerOrDrop(itemStack)
                    }
                }
                is ApiResponse.Error -> {}
            }
        }
    }

    override fun shutdown() {}
}