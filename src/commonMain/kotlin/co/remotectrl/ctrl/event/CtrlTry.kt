package co.remotectrl.ctrl.event

sealed class CtrlTry<T> {

    companion object {
        inline operator fun <TAggregate : CtrlAggregate<TAggregate>> invoke(
            execution: CtrlExecution<TAggregate, CtrlEvent<TAggregate>, CtrlInvalidation>
        ): CtrlTry<CtrlEvent<TAggregate>> {
            return when (execution) {
                is CtrlExecution.Validated -> {
                    Success(execution.event)
                }
                is CtrlExecution.Invalidated -> {
                    Failure(
                        Exception(
                            "validation failed:" + execution.items.map {
                                "\n- ${it.description}"
                            }
                        )
                    )
                }
            }
        }

        inline operator fun <T> invoke(func: () -> T): CtrlTry<T> =
            try {
                Success(func())
            } catch (error: Exception) {
                Failure(error)
            }
    }

    abstract fun <R> map(transform: (T) -> R): CtrlTry<R>
    abstract fun <R> flatMap(func: (T) -> CtrlTry<R>): CtrlTry<R>

    data class Success<T>(val result: T) : CtrlTry<T>() {
        override inline fun <R> map(transform: (T) -> R): CtrlTry<R> = CtrlTry { transform(result) }
        override inline fun <R> flatMap(func: (T) -> CtrlTry<R>): CtrlTry<R> = CtrlTry {
            func(result)
        }.let {
            when (it) {
                is Success -> it.result
                is Failure -> it as CtrlTry<R>
            }
        }
    }

    data class Failure<T>(val error: Exception) : CtrlTry<T>() {
        override inline fun <R> map(transform: (T) -> R): CtrlTry<R> = this as CtrlTry<R>
        override inline fun <R> flatMap(func: (T) -> CtrlTry<R>): CtrlTry<R> = this as CtrlTry<R>
    }
}
