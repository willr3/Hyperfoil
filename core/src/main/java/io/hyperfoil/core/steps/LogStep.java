package io.hyperfoil.core.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kohsuke.MetaInfServices;

import io.hyperfoil.api.config.BenchmarkDefinitionException;
import io.hyperfoil.api.config.ListBuilder;
import io.hyperfoil.api.config.Name;
import io.hyperfoil.api.config.Step;
import io.hyperfoil.api.config.StepBuilder;
import io.hyperfoil.api.session.Access;
import io.hyperfoil.api.session.Session;
import io.hyperfoil.api.session.Session.VarType;
import io.hyperfoil.core.builders.BaseStepBuilder;
import io.hyperfoil.core.session.SessionFactory;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This is a debugging step.
 */
public class LogStep implements Step {
   private static final Logger log = LoggerFactory.getLogger(LogStep.class);

   private final String message;
   private final Access[] vars;

   public LogStep(String message, List<String> vars) {
      this.message = message;
      this.vars = vars.stream().map(SessionFactory::access).toArray(Access[]::new);
   }

   @Override
   public boolean invoke(Session session) {
      // Normally we wouldn't allocate objects but since this should be used for debugging...
      if (vars.length == 0) {
         log.info(message);
      } else {
         Object[] objects = new Object[vars.length];
         for (int i = 0; i < vars.length; ++i) {
            Session.Var var = vars[i].getVar(session);
            if (!var.isSet()) {
               objects[i] = "<not set>";
            } else if (var.type() == VarType.OBJECT) {
               objects[i] = var.objectValue(session);
            } else if (var.type() == VarType.INTEGER) {
               objects[i] = var.intValue(session);
            } else {
               objects[i] = "<unknown type>";
            }
         }
         log.info(message, objects);
      }
      return true;
   }

   /**
    * Log a message and variable values.
    */
   @MetaInfServices(StepBuilder.class)
   @Name("log")
   public static class Builder extends BaseStepBuilder<Builder> {
      String message;
      List<String> vars = new ArrayList<>();

      /**
       * Message format pattern. Use <code>{}</code> to mark the positions for variables in the logged message.
       *
       * @param message Message format pattern.
       * @return Self.
       */
      public Builder message(String message) {
         this.message = message;
         return this;
      }

      /**
       * List of variables to be logged.
       *
       * @return Builder.
       */
      public ListBuilder vars() {
         return vars::add;
      }

      public Builder addVar(String var) {
         vars.add(var);
         return this;
      }

      @Override
      public List<Step> build() {
         if (message == null) {
            throw new BenchmarkDefinitionException("Missing message");
         }
         return Collections.singletonList(new LogStep(message, vars));
      }
   }
}
