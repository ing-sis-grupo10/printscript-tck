package implementation;

import implementation.handlers.PrintEmitterStream;
import interpreter.ErrorHandler;
import interpreter.InputProvider;
import interpreter.PrintEmitter;
import interpreter.PrintScriptInterpreter;

import printscript.common.Version;
import printscript.common.result.Failure;
import printscript.common.result.Result;
import printscript.interpreter.InterpreterFactory;
import printscript.interpreter.runtime.InputSource;
import printscript.lexer.LexerFactory;
import printscript.parser.ParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class MyPrintScriptInterpreter implements PrintScriptInterpreter {

    @Override
    public void execute(InputStream src, String version, PrintEmitter emitter, ErrorHandler handler, InputProvider provider) {
        try {
            Version languageVersion = Version.from(version);
            Reader reader = new InputStreamReader(src, StandardCharsets.UTF_8);
            InputSource inputSource = prompt -> {
                emitter.print(prompt);
                return provider.input(prompt);
            };

            var tokens = LexerFactory.create(reader, languageVersion);
            var statements = ParserFactory.create(tokens, languageVersion);
            var interpreter = InterpreterFactory.create(
                    statements, languageVersion, new PrintEmitterStream(emitter), inputSource);

            while (interpreter.hasNext()) {
                Result<?> result = interpreter.next();
                if (result instanceof Failure<?> failure) {
                    failure.diagnostics().forEach(d -> handler.reportError(d.message()));
                }
            }
        } catch (OutOfMemoryError e) {
            handler.reportError(e.getMessage());
        }
    }
}