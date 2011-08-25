/*
 * generated by Xtext
 */
package org.eclipse.emf.ecore.xcore.generator

import com.google.inject.Inject
import org.eclipse.emf.codegen.ecore.generator.Generator
import org.eclipse.emf.codegen.ecore.genmodel.GenModel
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenBaseGeneratorAdapter
import org.eclipse.emf.common.util.BasicMonitor
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.xcore.XOperation
import org.eclipse.emf.ecore.xcore.XPackage
import org.eclipse.emf.ecore.xcore.mappings.XcoreMapper
import org.eclipse.xtext.generator.IFileSystemAccess
import org.eclipse.xtext.generator.IGenerator
import org.eclipse.xtext.xbase.compiler.StringBuilderBasedAppendable
import org.eclipse.xtext.xbase.compiler.XbaseCompiler

import static extension org.eclipse.xtext.xtend2.lib.EObjectExtensions.*
import com.google.inject.Provider

class XcoreGenerator implements IGenerator {
	
	@Inject
	extension XcoreMapper mappings
	
	@Inject
	XbaseCompiler compiler
	
	@Inject
	Provider<XcoreGeneratorImpl> xcoreGeneratorImplProvider
	
	override void doGenerate(Resource resource, IFileSystemAccess fsa) {
		val pack = resource.contents.head as XPackage
		// install operation bodies
		for (op : pack.allContentsIterable.filter(typeof(XOperation))) {
			val eOperation = op.mapping.EOperation
			val appendable = new StringBuilderBasedAppendable()
			appendable.declareVariable(mappings.getMapping(op).jvmOperation.declaringType,"this");
//			val expectedType = op.jvmOperation.returnType
			compiler.compile(op.body, appendable, null)
			eOperation.EAnnotations.add(createGenModelAnnotation("body", appendable.toString))
		}
		
		generateGenModel(resource.contents.filter(typeof(GenModel)).head, fsa)
	}
	
	def generateGenModel(GenModel genModel, IFileSystemAccess fsa) {
		genModel.canGenerate = true
		val generator = xcoreGeneratorImplProvider.get
		generator.input = genModel
		generator.fileSystemAccess = fsa
		generator.generate(genModel, GenBaseGeneratorAdapter::MODEL_PROJECT_TYPE,
				new BasicMonitor());
	}
	
	def createGenModelAnnotation(String key, String value) {
		val result = EcoreFactory::eINSTANCE.createEAnnotation
		result.source = GenModelPackage::eNS_URI
		result.details.put(key, value)
		return result
	}
}
