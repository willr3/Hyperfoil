package io.hyperfoil.core.generators;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.kohsuke.MetaInfServices;

import io.hyperfoil.api.config.BenchmarkDefinitionException;
import io.hyperfoil.api.config.Locator;
import io.hyperfoil.api.config.Name;
import io.hyperfoil.api.config.PairBuilder;
import io.hyperfoil.api.config.Step;
import io.hyperfoil.api.config.StepBuilder;
import io.hyperfoil.api.session.Access;
import io.hyperfoil.api.session.ResourceUtilizer;
import io.hyperfoil.api.session.Session;
import io.hyperfoil.core.builders.BaseStepBuilder;
import io.hyperfoil.core.session.SessionFactory;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A class that will initialise, build and randomly select a single row of data.
 * The row is exposed as columns.
 */
public class RandomCsvRowStep implements Step, ResourceUtilizer {
   private static final Logger log = LoggerFactory.getLogger(RandomCsvRowStep.class);
   private String[][] rows;
   private final Access[] columnVars;

   public RandomCsvRowStep(String[][] rows, List<String> vars) {
      this.rows = rows;
      this.columnVars = vars.stream().map(SessionFactory::access).toArray(Access[]::new);
   }

   @Override
   public boolean invoke(Session session) {
      // columns provided by csv
      ThreadLocalRandom random = ThreadLocalRandom.current();
      int next = random.nextInt(rows.length);
      int last = columnVars.length;
      for (int i = 0, j = 0; i < last; i += 1) {
         if (rows[next][i] != null) {
            columnVars[j++].setObject(session, rows[next][i]);
         }
      }
      return true;
   }

   @Override
   public void reserve(Session session) {
      Arrays.asList(columnVars).forEach(var -> var.declareObject(session));
   }

   /**
    * Stores random row from a CSV-formatted file to variables.
    */
   @MetaInfServices(StepBuilder.class)
   @Name("randomCsvRow")
   public static class Builder extends BaseStepBuilder<Builder> {
      private Locator locator;
      private String file;
      private boolean skipComments;
      private boolean removeQuotes;
      private Map<String, Integer> builderColumns = new HashMap<>();
      private int maxSize = 0;

      @Override
      public Builder setLocator(Locator locator) {
         this.locator = locator;
         return this;
      }

      @Override
      public Builder copy(Locator locator) {
         Builder builder = new Builder().setLocator(locator)
               .file(file).skipComments(skipComments).removeQuotes(removeQuotes);
         builderColumns.forEach((column, position) -> builder.columns().accept(String.valueOf(position), column));
         return builder;
      }

      @Override
      public List<Step> build() {
         Predicate<String> comments = s -> (!skipComments || !(s.trim().startsWith("#")));
         List<String[]> rows;
         try (InputStream inputStream = locator.benchmark().data().readFile(file)) {
            if (inputStream == null) {
               throw new BenchmarkDefinitionException("Cannot load file " + file);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            rows = reader.lines()
                  .filter(comments)
                  .map(s -> removeQuotes ? s.replaceAll("\"", "") : s)
                  .map(line -> line.split(","))
                  .collect(Collectors.toList());
         } catch (IOException ioe) {
            throw new BenchmarkDefinitionException(ioe.getMessage());
         }
         if (rows.isEmpty()) {
            throw new BenchmarkDefinitionException("Missing CSV row data. Rows were not detected after initial processing of file.");
         }
         rows.forEach(arr -> {
            for (int i = 0; i < arr.length; i += 1) {
               if (!builderColumns.containsValue(i)) {
                  arr[i] = null;
               }
            }
         });
         List<String> cols = new ArrayList<>();
         cols.addAll(builderColumns.keySet());

         return Collections.singletonList(new RandomCsvRowStep(rows.toArray(new String[][]{}), cols));
      }

      /**
       * Defines mapping from columns to session variables.
       *
       * @return Builder.
       */
      public ColumnsBuilder columns() {
         return new ColumnsBuilder();
      }

      /**
       * Path to the CSV file that should be loaded.
       *
       * @param file Path to file.
       * @return Self.
       */
      public Builder file(String file) {
         this.file = file;
         return this;
      }

      /**
       * Skip lines starting with character '#'.
       *
       * @param skipComments Skip?
       * @return Self.
       */
      public Builder skipComments(boolean skipComments) {
         this.skipComments = skipComments;
         return this;
      }

      /**
       * Automatically unquote the columns.
       *
       * @param removeQuotes Remove?
       * @return Self.
       */
      public Builder removeQuotes(boolean removeQuotes) {
         this.removeQuotes = removeQuotes;
         return this;
      }

      public class ColumnsBuilder extends PairBuilder.OfString {
         /**
          * Use 0-based column as the key and variable name as the value.
          *
          * @param position  0-based column number.
          * @param columnVar Variable name.
          */
         @Override
         public void accept(String position, String columnVar) {
            Integer pos = Integer.parseInt(position);
            maxSize = (maxSize > pos ? maxSize : pos);
            if ((builderColumns.put(columnVar, pos) != null)) {
               throw new BenchmarkDefinitionException("Duplicate item '" + columnVar + "' in randomCSVrow step!");
            }
         }
      }
   }
}
