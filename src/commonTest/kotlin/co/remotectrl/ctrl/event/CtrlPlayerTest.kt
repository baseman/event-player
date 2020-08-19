package co.remotectrl.ctrl.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlPlayerTest {

    @Test
    fun `given Event when played via Player then apply changes`() {
        val player = getPlayer()

        val played = player.playFor(
            event = StubChangedEvent(EventLegend("1", "1", 1))
        )

        assertTrue(played is CtrlTry.Success)

        assertEquals(player.mutable.aggregate.legend.aggregateId.value, "1")
        assertEquals(player.mutable.aggregate.legend.latestVersion, 1)
        assertEquals(player.mutable.aggregate.changeVal, 1)
    }

    @Test
    fun `given Events when played via Player then apply changes`() {
        val player = getPlayer()

        val played = player.playForEvents(
            events = listOf(
                StubChangedEvent(EventLegend("1", "1", 1)),
                StubChangedEvent(EventLegend("2", "1", 2))
            )
        )

        assertTrue(played is CtrlTry.Success)

        assertEquals(player.mutable.aggregate.legend.aggregateId.value, "1")
        assertEquals(player.mutable.aggregate.legend.latestVersion, 2)
        assertEquals(player.mutable.aggregate.changeVal, 2)
    }

    @Test
    fun `given valid Command when played via Player then apply changes`() {
        val player = getPlayer()

        player.playFor(
            command = StubChangeCommand()
        )

        assertEquals(player.mutable.aggregate.legend.aggregateId.value, "1")
        assertEquals(player.mutable.aggregate.legend.latestVersion, 1)
        assertEquals(player.mutable.aggregate.changeVal, 1)
    }

    @Test
    fun `given valid Commands when played via Player then apply changes`() {
        val player = getPlayer()

        val played = player.playForCommands(
            commands = listOf(
                StubChangeCommand(),
                StubChangeCommand()
            )
        )

        assertTrue(played is CtrlTry.Success)

        assertEquals(player.mutable.aggregate.legend.aggregateId.value, "1")
        assertEquals(player.mutable.aggregate.legend.latestVersion, 2)
        assertEquals(player.mutable.aggregate.changeVal, 2)
    }

    @Test
    fun `given invalid Command when played via Player then apply changes`() {
        val player = getPlayer(stubMaxVal)

        val played = player.playFor(
            command = StubChangeCommand() // cannot increment higher
        )

        assertTrue(played is CtrlTry.Failure)

        assertEquals(player.mutable.aggregate.legend.aggregateId.value, "1")
        assertEquals(player.mutable.aggregate.legend.latestVersion, 0)
        assertEquals(player.mutable.aggregate.changeVal, stubMaxVal)
    }

    @Test
    fun `given invalid Commands when played via Player then apply changes`() {
        val player = getPlayer()

        val played = player.playForCommands(
            commands = listOf(
                StubCountIteratorCommand(),
                StubCountIteratorCommand(),
                StubCountIteratorCommand(), // cannot increment higher
                StubCountIteratorCommand()
            )
        )

        assertTrue(played is CtrlTry.Failure)

        assertEquals(player.mutable.aggregate.legend.aggregateId.value, "1")
        assertEquals(player.mutable.aggregate.legend.latestVersion, stubMaxVal)
        assertEquals(player.mutable.aggregate.changeVal, 2)
    }

    private fun getPlayer(changeVal: Int = 0): CtrlPlayer<StubAggregate> {
        return CtrlPlayer(
            aggregate = StubAggregate(
                legend = AggregateLegend("1", 0),
                changeVal = changeVal
            )
        )
    }
}

private const val stubMaxVal = 2
var cmdAttemptCountIterator = 0
class StubCountIteratorCommand : StubChangeCommand() {
    override fun validate(aggregate: StubAggregate, validation: CtrlValidation) {
        if (cmdAttemptCountIterator > stubMaxVal) {
            throw Exception("tried to continue parsing commands after initial failure")
        }
        super.validate(aggregate, validation)
        cmdAttemptCountIterator++
    }
}

open class StubChangeCommand : CtrlCommand<StubAggregate> {
    override fun getEvent(eventLegend: EventLegend<StubAggregate>): CtrlEvent<StubAggregate> {
        return StubChangedEvent(eventLegend)
    }

    override fun validate(aggregate: StubAggregate, validation: CtrlValidation) {
        validation.assert(
            { aggregate.changeVal < stubMaxVal },
            "stub value cannot be more than 3 for failure scenario"
        )
    }
}

data class StubChangedEvent(override val legend: EventLegend<StubAggregate>) : CtrlEvent<StubAggregate> {
    override fun applyChangesTo(
        aggregate: StubAggregate,
        latestVersion: Int
    ): StubAggregate = aggregate.copy(
        legend = aggregate.legend.copy(
            latestVersion = latestVersion
        ),
        changeVal = aggregate.changeVal + 1
    )
}

data class StubAggregate(
    override val legend: AggregateLegend<StubAggregate>,
    val changeVal: Int
) : CtrlAggregate<StubAggregate>
