/*******************************************************************************
 * Copyright (c) 2001, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jens Lukowski/Innoopract - initial renaming/restructuring
 *     
 *******************************************************************************/


package org.eclipse.wst.dtd.ui.dnd;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.wst.common.ui.dnd.DefaultDragAndDropCommand;
import org.eclipse.wst.dtd.core.DTDFile;
import org.eclipse.wst.dtd.core.DTDNode;
import org.eclipse.wst.dtd.core.TopLevelNode;
import org.eclipse.wst.dtd.ui.internal.DTDUIPlugin;

public class DragTopLevelNodesCommand extends DefaultDragAndDropCommand {

	static private final String moveNode = DTDUIPlugin.getResourceString("_UI_MOVE_NODE"); //$NON-NLS-1$
	static private final String moveNodes = DTDUIPlugin.getResourceString("_UI_MOVE_NODES"); //$NON-NLS-1$

	public DragTopLevelNodesCommand(Object target, float location, int operations, int operation, Collection sources) {
		super(target, location, operations, operation, sources);
	}

	public boolean canExecute() {
		if (!(target instanceof TopLevelNode)) {
			return false;
		}

		Iterator iter = sources.iterator();
		while (iter.hasNext()) {
			Object source = iter.next();
			if (!(source instanceof TopLevelNode)) {
				return false;
			}
		}
		return true;
	}

	public void execute() {
		DTDNode referenceNode = (DTDNode) target;

		DTDFile dtdFile = referenceNode.getDTDFile();
		dtdFile.getDTDModel().beginRecording(this, sources.size() > 1 ? moveNodes : moveNode);
		Iterator iter = sources.iterator();
		while (iter.hasNext()) {
			DTDNode node = (DTDNode) iter.next();
			if (node instanceof TopLevelNode) {
				dtdFile.moveNode(this, referenceNode, node, isAfter());
			}
		}
		dtdFile.getDTDModel().endRecording(this);
	}
}
