package implementation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import interpreter.ErrorHandler;
import interpreter.PrintScriptLinter;

import printscript.analyzer.AnalyzerRules;
import printscript.analyzer.AnalyzerRulesLoader;
import printscript.analyzer.PrintScriptAnalyzer;
import printscript.common.Version;
import printscript.common.result.Failure;
import printscript.common.result.Result;
import printscript.interpreter.InterpreterFactory;
import printscript.lexer.LexerFactory;
import printscript.parser.ParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class MyPrintScriptLinter implements PrintScriptLinter {

    @Override
    public void lint(InputStream src, String version, InputStream config, ErrorHandler handler) {
        JsonObject original = JsonParser.parseReader(new InputStreamReader(config, StandardCharsets.UTF_8)).getAsJsonObject();
        AnalyzerRules rules = new AnalyzerRulesLoader().load(new StringReader(translate(original).toString()));

        Version languageVersion = Version.from(version);
        Reader reader = new InputStreamReader(src, StandardCharsets.UTF_8);
        PrintStream silentOut = new PrintStream(OutputStream.nullOutputStream());

        var tokens = LexerFactory.create(reader, languageVersion);
        var statements = ParserFactory.create(tokens, languageVersion);
        var interpreter = InterpreterFactory.create(statements, languageVersion, silentOut, prompt -> prompt);
        var analyzer = new PrintScriptAnalyzer(interpreter, rules);

        while (analyzer.hasNext()) {
            Result<?> result = analyzer.next();
            if (result instanceof Failure<?> failure) {
                failure.diagnostics().forEach(d -> handler.reportError(d.message()));
            }
        }
        analyzer.diagnostics().forEach(d -> handler.reportError(d.message()));
    }

    private JsonObject translate(JsonObject original) {
        JsonObject translated = new JsonObject();

        if (original.has("identifier_format")) {
            if ("snake case".equalsIgnoreCase(original.get("identifier_format").getAsString())) {
                translated.addProperty("identifier_format", "snake_case");
            }
        } else {
            translated.addProperty("identifier_case_enabled", false);
        }

        if (original.has("mandatory-variable-or-literal-in-println")) {
            translated.addProperty(
                    "println_identifier_or_literal_only",
                    original.get("mandatory-variable-or-literal-in-println").getAsBoolean());
        }

        if (original.has("mandatory-variable-or-literal-in-readInput")) {
            translated.addProperty(
                    "read_input_identifier_or_literal_only",
                    original.get("mandatory-variable-or-literal-in-readInput").getAsBoolean());
        }

        return translated;
    }
}