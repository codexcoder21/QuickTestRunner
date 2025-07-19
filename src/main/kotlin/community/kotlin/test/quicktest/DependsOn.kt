package community.kotlin.test.quicktest

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOn(val rule: String)
