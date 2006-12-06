/**
 * <copyright>
 *
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: JPatternDictionary.java,v 1.4 2006/12/06 03:49:43 marcelop Exp $
 */

package org.eclipse.emf.codegen.merge.java;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.codegen.merge.java.facade.FacadeVisitor;
import org.eclipse.emf.codegen.merge.java.facade.JAbstractType;
import org.eclipse.emf.codegen.merge.java.facade.JAnnotation;
import org.eclipse.emf.codegen.merge.java.facade.JAnnotationTypeMember;
import org.eclipse.emf.codegen.merge.java.facade.JCompilationUnit;
import org.eclipse.emf.codegen.merge.java.facade.JEnumConstant;
import org.eclipse.emf.codegen.merge.java.facade.JField;
import org.eclipse.emf.codegen.merge.java.facade.JImport;
import org.eclipse.emf.codegen.merge.java.facade.JInitializer;
import org.eclipse.emf.codegen.merge.java.facade.JMethod;
import org.eclipse.emf.codegen.merge.java.facade.JNode;
import org.eclipse.emf.codegen.merge.java.facade.JPackage;

/**
 * A dictionary of signatures and {@link JNode}s.
 */
public class JPatternDictionary extends FacadeVisitor
{
  protected static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.MULTILINE | Pattern.DOTALL);
  protected static final Object [] NO_ARGUMENTS = new Object[0];
  
  protected static final boolean DEBUG = JMerger.DEBUG;
  
  protected JControlModel controlModel;
  protected JPackage jPackage;
  
  protected Map<String, JAnnotation> annotationMap;
  protected Map<String, JAnnotationTypeMember> annotationTypeMemberMap;
  protected Map<String, JEnumConstant> enumConstantMap;

  protected Map<String, JImport> importMap;
  protected Map<String, JAbstractType> abstractTypeMap;
  protected Map<String, JInitializer> initializerMap;
  protected Map<String, JField> fieldMap;
  protected Map<String, JMethod> methodMap;
  protected Map<String, Collection<JNode>> markupMap;  
  protected Set<String> noImportSet;
  
  public JPatternDictionary(JCompilationUnit compilationUnit, JControlModel controlModel)
  {
    super();
    this.controlModel = controlModel;
    start(compilationUnit);
    if (DEBUG)
    {
      System.out.println("Maps of compilation unit: " + compilationUnit.getName());
      dumpMaps();
    }
  }
  
  public JPackage getJPackage()
  {
    return jPackage;
  }
  
  public Map<String, JImport> getImportMap()
  {
    if (importMap == null)
    {
      importMap = new HashMap<String, JImport>();
    }
    return importMap;
  }

  public Map<String, JAbstractType> getAbstractTypeMap()
  {
    if (abstractTypeMap == null)
    {
      abstractTypeMap = new HashMap<String, JAbstractType>();
    }
    return abstractTypeMap;
  }

  public Map<String, JInitializer> getInitializerMap()
  {
    if (initializerMap == null)
    {
      initializerMap = new HashMap<String, JInitializer>();
    }
    return initializerMap;
  }

  public Map<String, JField> getFieldMap()
  {
    if (fieldMap == null)
    {
      fieldMap = new HashMap<String, JField>();
    }
    return fieldMap;
  }

  public Map<String, JMethod> getMethodMap()
  {
    if (methodMap == null)
    {
      methodMap = new HashMap<String, JMethod>();
    }
    return methodMap;
  }  
  
  public Map<String, JAnnotation> getAnnotationMap()
  {
    if (annotationMap == null)
    {
      annotationMap = new HashMap<String, JAnnotation>();
    }
    return annotationMap;
  }
  
  public Map<String, JAnnotationTypeMember> getAnnotationTypeMemberMap()
  {
    if (annotationTypeMemberMap == null)
    {
      annotationTypeMemberMap = new HashMap<String, JAnnotationTypeMember>();
    }
    return annotationTypeMemberMap;
  }
  
  public Map<String, JEnumConstant> getEnumConstantMap()
  {
    if (enumConstantMap == null)
    {
      enumConstantMap = new HashMap<String, JEnumConstant>();
    }
    return enumConstantMap;
  }  
  
	public Map<String, Collection<JNode>> getMarkupMap()
  {
    if (markupMap == null)
    {
      markupMap = new HashMap<String, Collection<JNode>>();
    }
    return markupMap;
  }
  
  /**
   * Determines if the node is marked up based on the node and parent markup.
   * <p>
   * If both patterns are <code>null</code>, the node is marked up.
   * <p>
   * If both patterns are not <code>null</code>, the node is marked up if the node itself and its 
   * parent is marked up. If the node does not have a parent, the node is marked up if the node itself is marked up. 
   * <p>
   * If only <code>markupPattern</code> or <code>parentMarkupPattern</code> is set, the node is marked up 
   * if the node or its parent is marked up respectively.
   * 
   * @param markupPattern
   * @param parentMarkupPattern
   * @param node
   * @return
   * 
   * @see #isMarkedUp(Pattern, JNode) 
   */
  public boolean isMarkedUp(Pattern markupPattern, Pattern parentMarkupPattern, JNode node)
  {
    if (node.getParent() == null) // if node does not have a parent, check only markupPattern
    {
      return isMarkedUp(markupPattern, node);
    }
    else if (markupPattern != null && parentMarkupPattern != null) // if both patterns are set, check both
    {
      return isMarkedUp(markupPattern, node) && isMarkedUp(parentMarkupPattern, node.getParent());
    }
    else if (markupPattern != null) // if only markupPattern is set
    {
      return isMarkedUp(markupPattern, node);
    }     
    else if (parentMarkupPattern != null) // if only parentMarkupPattern is set
    {
      return isMarkedUp(parentMarkupPattern, node.getParent());
    }
    else // both patterns are null
    {
      return true;
    }
  }  
  
  public boolean isMarkedUp(Pattern markupPattern, JNode node)
  {
    if (markupPattern == null)
    {
      return true;
    }
    else
    {
      for (Map.Entry<String, Collection<JNode>> markupEntry : getMarkupMap().entrySet())
      {
        String key = markupEntry.getKey();
        if (key != null && markupPattern.matcher(key).find())
        {
          if (markupEntry.getValue().contains(node))
          {
            return true;
          }
        }
      }
      return false;
    }
  }
  
  protected Set<String> getNoImporterSet()
  {
    if (noImportSet == null)
    {
      noImportSet = new HashSet<String>();
    }
    return noImportSet;
  }

  public boolean isNoImport(JImport jImport)
  {
    return noImportSet != null && getNoImporterSet().contains(jImport.getQualifiedName());
  }
    
  @Override
  protected boolean visit(JCompilationUnit compilationUnit)
  {
    if (controlModel.getNoImportPattern() != null)
    {
      Matcher matcher = controlModel.getNoImportPattern().matcher(compilationUnit.getContents());
      while (matcher.find())
      {
        getNoImporterSet().add(matcher.group(1));
      }
    }
    return super.visit(compilationUnit);
  }
  
  @Override
  protected boolean visit(JPackage jPackage)
  {
    this.jPackage = jPackage;
    return super.visit(jPackage);
  }
  
  @Override
  protected boolean visit(JAbstractType abstractType)
  {
    getAbstractTypeMap().put(abstractType.getQualifiedName(), abstractType);
    return super.visit(abstractType);
  }  
  
  @Override
  protected boolean visit(JImport jImport)
  {
    getImportMap().put(jImport.getQualifiedName(), jImport);
    return super.visit(jImport);
  }

  @Override
  protected boolean visit(JInitializer initializer)
  {
    getInitializerMap().put(initializer.getQualifiedName(), initializer);
    return super.visit(initializer);
  }
  
  @Override
  protected boolean visit(JField field)
  {
    getFieldMap().put(field.getQualifiedName(), field);
    return super.visit(field);
  }
  
  @Override
  protected boolean visit(JMethod method)
  {
    getMethodMap().put(method.getQualifiedName(), method);
    return super.visit(method);
  }
    
  @Override
  protected boolean visit(JAnnotation annotation)
  {
    getAnnotationMap().put(annotation.getQualifiedName(), annotation);
    return super.visit(annotation);
  }
  
  @Override
  protected boolean visit(JAnnotationTypeMember annotationTypeMember)
  {
    getAnnotationTypeMemberMap().put(annotationTypeMember.getQualifiedName(), annotationTypeMember);
    return super.visit(annotationTypeMember);
  }
  
  @Override
  protected boolean visit(JEnumConstant enumConstant)
  {
    getEnumConstantMap().put(enumConstant.getQualifiedName(), enumConstant);
    return super.visit(enumConstant);
  }
  
  @Override
  protected void beforeVisit(JNode node)
  {
    for (JControlModel.DictionaryPattern dictionaryPattern : controlModel.getDictionaryPatterns())
    {
      if (dictionaryPattern.getSelectorFeature().getFeatureClass().isInstance(node))
      {
        try
        {
          String selection = (String)dictionaryPattern.getSelectorFeature().getFeatureMethod().invoke(node, NO_ARGUMENTS);
          selection = checkSelection(selection, dictionaryPattern, node);
          if (selection != null)
          {
            markupNode(selection, dictionaryPattern, node);
          }
        }
        catch (IllegalAccessException exception)
        {
          if (DEBUG)
          {
            exception.printStackTrace();
          }
        }
        catch (InvocationTargetException exception)
        {
          if (DEBUG)
          {
            exception.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * Checks the selection to fix the problem with JDOM that assigns wrong javadoc to the node.
   * 
   * @param selection
   * @param dictionaryPattern
   * @param node
   * @return <code>null</code> if the node should be skipped 
   */
  protected String checkSelection(String selection, JControlModel.DictionaryPattern dictionaryPattern, JNode node)
  {
    if (dictionaryPattern.getSelectorFeature().getFeatureMethod().getName().equals("getComment"))
    {
      String contents = node.getContents();
      for (int start = 0, end = contents.length(), count = 0; start < end; )
      {
        Matcher matcher = COMMENT.matcher(contents.subSequence(start, end));
        if (matcher.find())
        {
          // Ignore it if there are multiple comments.
          //
          if (++count > 1)
          {
            int braceIndex = contents.indexOf("{", start); // }
            if (braceIndex > start + matcher.start(0))
            {
              return null;
            }

            break;
          }
          start += matcher.end(0) + 1;
        }
        else
        {
          break;
        }
      }
    }
    return selection;
  }
  
  /**
   * Matches pattern in dictionary pattern against selection, and marks up node
   * with all matching groups.
   * <p>
   * If pattern matches selection, but no groups are defined, node is marked up with 
   * dictionary pattern name. 
   * 
   * @param selection
   * @param dictionaryPattern
   * @param node
   */
  protected void markupNode(String selection, JControlModel.DictionaryPattern dictionaryPattern, JNode node)
  {
    Matcher matcher = dictionaryPattern.getPattern().matcher(selection);
    if (matcher.find())
    {
      if (matcher.groupCount() > 0)
      {
        for (int i = 1; i <= matcher.groupCount(); ++i)
        {
          String markup = matcher.group(i);
          markupNode(markup, node);
        }
      }
      else
      {
        // if there are no groups defined or matched, but the whole pattern matches,
        // then markup nodes with pattern name
        String markup = dictionaryPattern.getName();
        if (markup != null && !"".equals(markup))
        {
          markupNode(markup, node);
        }
      }
    }
  }

  /**
   * Marks up node with the given markup string.
   * 
   * @param markup
   * @param node
   */
  protected void markupNode(String markup, JNode node)
  {
    Collection<JNode> collection = getMarkupMap().get(markup);
    if (collection == null)
    {
      collection = new HashSet<JNode>();
      getMarkupMap().put(markup, collection);
    }
    collection.add(node);
  }

//  /**
//  * Markup node same as the other node if the markup pattern in dictionary pattern 
//  * matches markup of other node.
//  * <br>
//  * This method allows copying markup from parent using Node/getParent pull rule.  
//  *   
//  * @param node
//  * @param dictionaryPattern
//  * @param otherNode
//  */
//  private void addMarkupFromOtherNode(JNode node, JControlModel.DictionaryPattern dictionaryPattern, JNode otherNode)
//  {
//   Pattern markupPattern = dictionaryPattern.getPattern();
//   for (Map.Entry<String, Collection<JNode>> markupEntry : getMarkupMap().entrySet())
//   {
//     String key = markupEntry.getKey();
//     if (key != null && markupPattern.matcher(key).find())
//     {
//       Collection<JNode> markedUpNodes = markupEntry.getValue();
//       if (markedUpNodes.contains(otherNode))
//       {
//         markedUpNodes.add(node);
//       }
//     }
//   }
//  }  
    
  protected void dumpMaps()
  {
    try
    {
      Field[] fields = this.getClass().getDeclaredFields();
      for (int i = 0; i < fields.length; i++)
      {
        if (fields[i].getName().endsWith("Map") && Map.class.isAssignableFrom(fields[i].getType()))
        {
          Map< ? , ? > map = (Map< ? , ? >)fields[i].get(this);
          String mapString = String.format("%s = %s\n", fields[i].getName(), dumpMap("\t", map));
          System.out.print(mapString);
        }
      }
    }
    catch (Exception e)
    {
      if (DEBUG)
      {
        e.printStackTrace();
      }
    }
  }

  protected static <K, V> String dumpMap(String lineIndent, Map<K, V> map)
  {
    int columnWidth = 10;
    if (map != null)
    {
      StringBuffer sb = new StringBuffer("\n");
      for (Map.Entry<K, V> entry : map.entrySet())
      {
        K key = entry.getKey();
        String keyString = (key == null ? null : key.toString());
        if (keyString != null && columnWidth < keyString.length())
        {
          columnWidth = keyString.length() + 25;
        }
        sb.append(String.format("%s%-" + columnWidth + "s = ", lineIndent, keyString));
        if (entry.getValue() instanceof Collection)
        {
          sb.append("\n");
          Collection<?> values = (Collection<?>)entry.getValue();
          for (Object element : values)
          {
            sb.append(String.format("%s%<s%s\n", lineIndent, element.toString()));
          }
        }
        else
        {
          sb.append(String.format("%s\n", entry.getValue()));
        }
      }
      return sb.toString();
    }
    else
    {
      return null;
    }
  }  
}
