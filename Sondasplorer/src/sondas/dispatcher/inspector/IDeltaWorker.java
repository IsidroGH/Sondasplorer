package sondas.dispatcher.inspector;

import sondas.Metadata;
import sondas.inspector.delta.DeltaTree;

public interface IDeltaWorker {
	public void execute(DeltaTree tree);
	public void executeMetadata(Metadata metadata);
}
