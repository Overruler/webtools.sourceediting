/*******************************************************************************
 * Copyright (c) 2001, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.validation.core.errorinfo;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.xml.core.internal.validation.core.ValidationMessage;


public class ReferencedFileErrorDialog extends Dialog
{
  protected TaskListTableViewer tableViewer;
  protected List errorList;
  protected String markedUpDetailsMessage;
  protected String contextFile;
  protected String referencedFile;
  protected StyledText styledText;
  protected Text fullFileNameField;
  
  protected ResourceBundle resourceBundle;

  public ReferencedFileErrorDialog(Shell parentShell, List errorList, String contextFile, String referencedFile)
  {
    super(parentShell);
    
    resourceBundle = ResourceBundle.getBundle("org.eclipse.wst.xml.ui.internal.validation.xmlvalidation");

    int styleBits = getShellStyle() | SWT.RESIZE;
    styleBits &= ~SWT.APPLICATION_MODAL;

    setShellStyle(styleBits);
    this.errorList = errorList;

    this.referencedFile = referencedFile;
    this.contextFile = contextFile;
    this.markedUpDetailsMessage = getMarkedUpDetailsMessage();
  }

  public int createAndOpen()
  {
    create();
    getShell().setText(resourceBundle.getString("_UI_REF_FILE_ERROR_DETAILS"));
	
    setBlockOnOpen(false);
    return open();
  }


  protected Control createButtonBar(Composite parent)
  {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    fullFileNameField = new Text(composite, SWT.NONE);
    fullFileNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    fullFileNameField.setBackground(parent.getBackground());
    fullFileNameField.setEditable(false);

    super.createButtonBar(composite);

    return composite;
  }

  protected Control createDialogArea(Composite parent)
  {
    Composite dialogArea = (Composite) super.createDialogArea(parent);
    dialogArea.setLayout(new GridLayout());

    Composite c = new Composite(dialogArea, SWT.NONE);
    c.setLayout(new GridLayout());
    c.setLayoutData(createGridData(true, -1, 200));

    styledText = new StyledText(c, SWT.MULTI | SWT.WRAP);
    styledText.setBackground(c.getBackground());
    setStyledText(styledText, markedUpDetailsMessage);
    styledText.setEditable(false);
    styledText.setLayoutData(createGridData(false, 650, -1));

    MouseListener mouseListener = new MouseListener();
    styledText.addMouseMoveListener(mouseListener);

    tableViewer = new TaskListTableViewer(c, 10);
    tableViewer.setInput(errorList);
    tableViewer.addSelectionChangedListener(new InternalSelectionListener());
    tableViewer.getControl().setLayoutData(createGridData(true, 700, -1));
    return dialogArea;
  }

  String getFullURI(int offset)
  {
    String uri = "";
    int index = getIndex(offset);
    if (index != -1)
    {
      if (index == 0 || index == 2 || index == 3)
      {
        uri = referencedFile;
      }
      else
      {
        uri = contextFile;
      }
    }
    return uri;
  }

  private int getIndex(int offset)
  {
    int result = -1;
    StyleRange[] range = styledText.getStyleRanges();
    for (int i = 0; i < range.length; i++)
    {
      int l = range[i].start;
      int r = l + range[i].length;
      if (l <= offset && r >= offset)
      {
        result = i;
        break;
      }
    }
    return result;
  }

  class MouseListener implements MouseMoveListener
  {
    public void mouseMove(MouseEvent event)
    {
      String toolTipText = "";
      try
      {

        int offset = styledText.getOffsetAtLocation(new Point(event.x, event.y));
        toolTipText = getFullURI(offset);

      }
      catch (Exception e)
      {
    	// Do nothing.
      }
      styledText.setToolTipText(toolTipText);
      if (toolTipText != null && toolTipText.length() > 0)
      {
        fullFileNameField.setText(toolTipText);
      }
    }
  }

  private String getMarkedUpDetailsMessage()
  {
	String detailsMessage = "";  	
    // TODO... need to move '_UI_REF_FILE_ERROR_DESCRIPTION' to this plugin's properties file
    //			
      String string = resourceBundle.getString("_UI_REF_FILE_ERROR_DESCRIPTION");
      // TODO... need to edit the properties file to remove "'" characters from the string
      // I'm using these characters to markup the bold font. It's safer if I add these programtically.
      //
	  string = removePattern(string, "'");

      String c = "'" + getLastSegment(contextFile) + "'";
      String r = "'" + getLastSegment(referencedFile) + "'";

      detailsMessage = MessageFormat.format(string, new Object[] { r, c, r, c });    
    return detailsMessage;
  }

  private String removePattern(String string, String pattern)
  {
  	while (true)
  	{
  		int index = string.indexOf(pattern);
  		if (index != -1)
  		{
  			string = string.substring(0, index) + string.substring(index + pattern.length());
  		}
  		else
  		{
  			break;
  		}
  	}
  	return string;  	
  }

  private void setStyledText(StyledText styledText, String text)
  {
    String visibleMessage = "";
    for (StringTokenizer st = new StringTokenizer(markedUpDetailsMessage, "'", false); st.hasMoreTokens();)
    {
      String token = st.nextToken();
      visibleMessage += token;
    }

    styledText.setText(visibleMessage);
   //dw Font font = styledText.getFont();

    boolean inQuote = false;
    int position = 0;
    for (StringTokenizer st = new StringTokenizer(markedUpDetailsMessage, "'", true); st.hasMoreTokens();)
    {
      String token = st.nextToken();

      if (token.equals("'"))
      {
        inQuote = !inQuote;
      }
      else
      {
        if (inQuote)
        {
          try
          {

            StyleRange style = new StyleRange(position, token.length(), styledText.getForeground(), styledText.getBackground(), SWT.BOLD);
            styledText.setStyleRange(style);
          }
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }
        position = position + token.length();
      }
    }
  }

  private static GridData createGridData(boolean fillBoth, int w, int h)
  {
    GridData gd = new GridData(fillBoth ? GridData.FILL_BOTH : GridData.FILL_HORIZONTAL);
    gd.widthHint = w;
    gd.heightHint = h;
    return gd;
  }
  
  private static String getLastSegment(String uri)
  {
    String result = uri;
    int index = Math.max(uri.lastIndexOf("/"), uri.lastIndexOf("\\"));
    if (index != -1)
    {
      result = uri.substring(index + 1);
    }
    return result;
  }

  protected class InternalSelectionListener implements ISelectionChangedListener
  {
    public void selectionChanged(SelectionChangedEvent event)
    {
      ISelection selection = event.getSelection();
      if (selection instanceof StructuredSelection)
      {
        ValidationMessage validationMessage = (ValidationMessage) ((StructuredSelection) selection).getFirstElement();
        if (validationMessage != null)
        {
          String uristring = validationMessage.getUri();
          ReferencedFileErrorUtility.openEditorAndGotoError(uristring, validationMessage.getLineNumber(), validationMessage.getColumnNumber());
        }
      }
    }
  }
}