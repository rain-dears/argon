package dev.lvstrng.vxe.event.events;

import dev.lvstrng.vxe.event.CancellableEvent;
import dev.lvstrng.vxe.event.Listener;

import java.util.ArrayList;

public interface AttackListener extends Listener {
	void onAttack(AttackEvent event);

	class AttackEvent extends CancellableEvent<AttackListener> {

		@Override
		public void fire(ArrayList<AttackListener> listeners) {
			listeners.forEach(e -> e.onAttack(this));
		}

		@Override
		public Class<AttackListener> getListenerType() {
			return AttackListener.class;
		}
	}
}
