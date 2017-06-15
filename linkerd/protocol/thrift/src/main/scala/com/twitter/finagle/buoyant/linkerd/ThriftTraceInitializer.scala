package com.twitter.finagle.buoyant.linkerd

import com.twitter.logging.Logger
import com.twitter.finagle._
import com.twitter.finagle.tracing.{Trace, TraceInitializerFilter, Tracer}

object ThriftTraceInitializer {
  val role = TraceInitializerFilter.role

  val serverModule: Stackable[ServiceFactory[Array[Byte], Array[Byte]]] =
    new Stack.Module1[param.Tracer, ServiceFactory[Array[Byte], Array[Byte]]] {
      val role = ThriftTraceInitializer.role
      val description = "Ensure that there is a trace id set"

      def make(_tracer: param.Tracer, next: ServiceFactory[Array[Byte], Array[Byte]]) = {
        val param.Tracer(tracer) = _tracer
        new ServerFilter(tracer) andThen next
      }
    }

  class ServerFilter(tracer: Tracer)
    extends SimpleFilter[Array[Byte], Array[Byte]] {

    def apply(req: Array[Byte], service: Service[Array[Byte], Array[Byte]]) = {
      val log = Logger.get(getClass.getName)
      log.info("Inside the tracing filter...")
      if (!Trace.hasId) {
        log.info("TraceId not present...creating one...")
        Trace.letTracerAndNextId(tracer) {
          log.info("Generated TraceId is %s", Trace.id.traceId)
          service(req)
        }
      } else {
        log.info("TraceId is present %s", Trace.id.traceId)
        service(req)
      }
    }
  }
}
