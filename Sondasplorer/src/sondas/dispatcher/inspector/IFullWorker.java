package sondas.dispatcher.inspector;

import sondas.inspector.Stamp;
import sondas.inspector.delta.DeltaTree;

public interface IFullWorker {
	public void execute(Stamp stamp);
}
