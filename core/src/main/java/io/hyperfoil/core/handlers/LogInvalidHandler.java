package io.hyperfoil.core.handlers;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hyperfoil.api.config.Name;
import io.hyperfoil.api.config.Step;
import io.hyperfoil.api.connection.HttpRequest;
import io.hyperfoil.api.http.BodyHandler;
import io.hyperfoil.api.http.HeaderHandler;
import io.hyperfoil.core.util.Util;
import io.hyperfoil.function.SerializableSupplier;
import io.netty.buffer.ByteBuf;

public class LogInvalidHandler implements BodyHandler, HeaderHandler {
   private Logger log = LoggerFactory.getLogger(LogInvalidHandler.class);

   @Override
   public void handleData(HttpRequest request, ByteBuf data) {
      if (!request.isValid()) {
         log.debug("#{}: {} {}/{}, {} bytes: {}", request.session.uniqueId(), request.method, request.authority, request.path, data.readableBytes(),
               Util.toString(data, data.readerIndex(), data.readableBytes()));
      }
   }

   @Override
   public void handleHeader(HttpRequest request, CharSequence header, CharSequence value) {
      if (!request.isValid()) {
         log.debug("#{}: {} {}/{}, {}: {}", request.session.uniqueId(), request.method, request.authority, request.path, header, value);
      }
   }

   /**
    * Logs body chunks from requests marked as invalid.
    */
   @MetaInfServices(BodyHandler.Builder.class)
   @Name("logInvalid")
   public static class BodyHandlerBuilder implements BodyHandler.Builder {
      @Override
      public LogInvalidHandler build(SerializableSupplier<? extends Step> step) {
         return new LogInvalidHandler();
      }
   }

   /**
    * Logs headers from requests marked as invalid.
    */
   @MetaInfServices(HeaderHandler.Builder.class)
   @Name("logInvalid")
   public static class HeaderHandlerBuilder implements HeaderHandler.Builder {
      @Override
      public LogInvalidHandler build(SerializableSupplier<? extends Step> step) {
         return new LogInvalidHandler();
      }
   }
}