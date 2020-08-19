package co.remotectrl.ctrl.event

class CtrlPlayer<TAggregate : CtrlAggregate<TAggregate>>(
    aggregate: TAggregate
) {
    val mutable = CtrlMutable(aggregate = aggregate)

    fun playFor(event: CtrlEvent<TAggregate>): CtrlTry<Unit> = CtrlTry.invoke {
        event.applyTo(mutable)
    }

    fun playForEvents(events: List<CtrlEvent<TAggregate>>): CtrlTry<Unit> {
        var error: CtrlTry<Unit>? = null
        for (evt in events) {
            when (val execution = playFor(evt)) {
                is CtrlTry.Success -> continue
                is CtrlTry.Failure -> {
                    error = execution
                    break
                }
            }
        }

        return error ?: CtrlTry.Success(Unit)
    }

    fun playFor(
        command: CtrlCommand<TAggregate>
    ): CtrlTry<Unit> = when (val execution = CtrlTry(command.executeOn(mutable.aggregate))) {
        is CtrlTry.Success -> playFor(event = execution.result)
        is CtrlTry.Failure -> CtrlTry.Failure(error = execution.error)
    }

    fun playForCommands(
        commands: List<CtrlCommand<TAggregate>>
    ): CtrlTry<Unit> {
        var error: CtrlTry<Unit>? = null
        for (cmd in commands) {
            when (val execution = playFor(cmd)) {
                is CtrlTry.Success -> continue
                is CtrlTry.Failure -> {
                    error = execution
                    break
                }
            }
        }

        return error ?: CtrlTry.Success(Unit)
    }
}
