package com.mds.lsp.tcl;

import com.google.common.base.Joiner;
import com.mds.lsp.tcl.diagnostic.Lints;
import com.mds.lsp.tcl.diagnostic.TclFileObject;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class TclTextDocumentService implements TextDocumentService {

    private static final Logger LOG = Logger.getLogger("main");

    private final CompletableFuture<LanguageClient> client;
    private final TclLanguageServer server;
    private final Map<URI, VersionedContent> activeDocuments = new HashMap<>();

    TclTextDocumentService(CompletableFuture<LanguageClient> client, TclLanguageServer server) {
        this.client = client;
        this.server = server;
    }

    /** Text of file, if it is in the active set */
    Optional<String> activeContent(URI file) {
        return Optional.ofNullable(activeDocuments.get(file)).map(doc -> doc.content);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams completionParams) {
        return null;
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem completionItem) {
        return null;
    }

    @Override
    public CompletableFuture<Hover> hover(TextDocumentPositionParams textDocumentPositionParams) {
        return null;
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams textDocumentPositionParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams textDocumentPositionParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams referenceParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams textDocumentPositionParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends SymbolInformation>> documentSymbol(DocumentSymbolParams documentSymbolParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends Command>> codeAction(CodeActionParams codeActionParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams codeLensParams) {
        return null;
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens codeLens) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams documentFormattingParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams documentRangeFormattingParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams documentOnTypeFormattingParams) {
        return null;
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams renameParams) {
        return null;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        TextDocumentItem document = params.getTextDocument();
        URI uri = URI.create(document.getUri());

        activeDocuments.put(uri, new VersionedContent(document.getText(), document.getVersion()));

        doLint(Collections.singleton(uri));
    }

    void doLint(Collection<URI> paths) {
        LOG.info("Lint " + Joiner.on(", ").join(paths));

        List<Diagnostic<? extends TclFileObject>> errors = new ArrayList<>();
        Map<URI, Optional<String>> content =
                paths.stream().collect(Collectors.toMap(f -> f, this::activeContent));
        DiagnosticCollector<TclFileObject> compile =
                server.configured().interpreter.evalBatch(content);

        errors.addAll(compile.getDiagnostics());

        publishDiagnostics(paths, errors);
    }

    private void publishDiagnostics(
            Collection<URI> touched,
            List<javax.tools.Diagnostic<? extends TclFileObject>> diagnostics) {
        Map<URI, PublishDiagnosticsParams> files =
                touched.stream()
                        .collect(
                                Collectors.toMap(
                                        uri -> uri,
                                        newUri ->
                                                new PublishDiagnosticsParams(
                                                        newUri.toString(), new ArrayList<>())));

        // Organize diagnostics by file
        for (javax.tools.Diagnostic<? extends TclFileObject> error : diagnostics) {
            URI uri = error.getSource().toUri();
            PublishDiagnosticsParams publish =
                    files.computeIfAbsent(
                            uri,
                            newUri ->
                                    new PublishDiagnosticsParams(
                                            newUri.toString(), new ArrayList<>()));
            Lints.convert(error).ifPresent(publish.getDiagnostics()::add);
        }

        // If there are no errors in a file, put an empty PublishDiagnosticsParams
        for (URI each : touched) files.putIfAbsent(each, new PublishDiagnosticsParams());

        files.forEach(
                (file, errors) -> {
                    if (touched.contains(file)) {
                        client.join().publishDiagnostics(errors);

                        LOG.info(
                                "Published "
                                        + errors.getDiagnostics().size()
                                        + " errors from "
                                        + file);
                    } else
                        LOG.info(
                                "Ignored "
                                        + errors.getDiagnostics().size()
                                        + " errors from not-open "
                                        + file);
                });
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {

    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {

    }

    @Override
    public void didSave(DidSaveTextDocumentParams didSaveTextDocumentParams) {

    }
}