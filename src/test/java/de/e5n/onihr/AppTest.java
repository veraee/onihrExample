package de.e5n.onihr;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {

	private static void llog(String s) {
		System.out.println("HBR:" + s);
	}

	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	public static class MyB {
		private int val = 22;
		private MyA mya = new MyA();

		public MyB() {
		}

		public MyB(int val, MyA mya) {
			this.val = val;
			this.mya = mya;
		}

		public MyA getMya() {
			return mya;
		}

		public void setMya(MyA mya) {
			this.mya = mya;
		}

		public int getVal() {
			return val;
		}

		public void setVal(int val) {
			this.val = val;
		}

	}

	public static class MyA {
		private int val = 888;

		public MyA() {

		}

		public MyA(int val) {
			this.val = val;
		}

		public int getVal() {
			return val;
		}

		public void setVal(int val) {
			this.val = val;
		}

	}

	/**
	 * Rigourous Test :-)
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void XtestApp() throws FileNotFoundException, IOException {
		// Creates and enters a Context. The Context stores information
		// about the execution environment of a script.
		Context cx = Context.enter();
		try {
			// Initialize the standard objects (Object, Function, etc.)
			// This must be done before scripts can be executed. Returns
			// a scope object that we use in later calls.
			Scriptable scope = cx.initStandardObjects();

			String handlebarsStr = IOUtils.toString(new FileInputStream("src\\lib\\handlebars-v4.0.5.js"));
			String s = "/* helo */" + handlebarsStr + "\n"
					+ " var template = Handlebars.compile(\"mumu{{mya.val}}muma\");"
					// + " var template =
					// Handlebars.compile(\"mumu{{this}}muma\");"
					+ " function f1(obj) { " + " var context = {vname: \"Poti\"};" + " var html = template(obj);"
					+ " return html;" + " }" + "var v=6; v;";

			Object result = cx.evaluateString(scope, s, "<cmd>", 1, null);
			System.out.println("result=" + result);

			MyB myb = new MyB();
			Object fObj = scope.get("f1", scope);

			Object functionArgs[] = { Context.javaToJS(myb, scope) };
			Function f = (Function) fObj;
			Object f1r = f.call(cx, scope, scope, functionArgs);
			String report = "f1 = " + Context.toString(f1r);
			System.out.println("report=" + report);

			// System.err.println(Context.toString(result));

		} finally {
			// Exit from the context.
			Context.exit();
		}
	}

	private ObjectPool<HbRender> pool;

	public void testTP() {
		pool = new GenericObjectPool<HbRender>(new HbRenderFactory());
		// ReaderUtil readerUtil = new ReaderUtil();
		// renderPool.borrowObject()

		for (int i = 0; i < 12; i++) {
			for (int j = 0; j < 2; j++) {

				HbRender hbrender = null;
				try {
					hbrender = pool.borrowObject();
					String tplName = "t"+(j+1);
					MyB modelObj = new MyB(2212, new MyA(9000*i));
					String fhtml = hbrender.render(tplName, modelObj);
					System.out.println("fhtml=" + fhtml);
				} catch (Exception e) {
					throw new RuntimeException("Unable to borrow buffer from pool" + e.toString());
				} finally {
					try {
						if (null != hbrender) {
							pool.returnObject(hbrender);
						}
					} catch (Exception e) {
						// ignored
					}
				}
			}
		}

	}

	public static class HbRender {
		private Scriptable scope;

		public HbRender() {
			llog("new HbRender");
		}

		public void prepare() throws Exception {
			Context cx = Context.enter();
			try {
				scope = cx.initStandardObjects();

				String handlebarsStr = IOUtils.toString(new FileInputStream("src\\lib\\handlebars-v4.0.5.js"));
				String s = "/* helo */" + handlebarsStr + "\n"
						+ " var template1 = Handlebars.compile(\"mumu1{{mya.val}}muma\");"
						+ " var template2 = Handlebars.compile(\"mumu2{{mya.val}}muma\");"
						// + " var template =
						// Handlebars.compile(\"mumu{{this}}muma\");"
						+ " function f1(tplname, obj) { "
						+ " var html = (''+tplname) === 't1' ? template1(obj) : template2(obj);" + " return html;"
						+ " }" + "true;";

				Object result = cx.evaluateString(scope, s, "<cmd>", 1, null);
				llog("prepare-result=" + result);

			} finally {
				// Exit from the context.
				Context.exit();
			}
		}

		public String render(String tplName, MyB myB) {
			Context cx = Context.enter();
			try {
				Object fObj = scope.get("f1", scope);

				Object functionArgs[] = { tplName, Context.javaToJS(myB, scope) };
				Function f = (Function) fObj;
				Object f1r = f.call(cx, scope, scope, functionArgs);
				String fResult = "f1|" + Context.toString(f1r) + "| ";
				llog("render-fResult=" + fResult);
				return fResult;
			} finally {
				// Exit from the context.
				Context.exit();
			}
		}
	}

	public static class HbRenderFactory extends BasePooledObjectFactory<HbRender> {

		/**
		 * Creates an object instance, to be wrapped in a {@link PooledObject}.
		 * <p>
		 * This method <strong>must</strong> support concurrent, multi-threaded
		 * activation.
		 * </p>
		 * 
		 * @throws Exception
		 */
		@Override
		public HbRender create() throws Exception {
			HbRender ret = new HbRender();
			ret.prepare();
			//
			return ret;
		}

		/**
		 * Use the default PooledObject implementation.
		 */
		@Override
		public PooledObject<HbRender> wrap(HbRender buffer) {
			return new DefaultPooledObject<HbRender>(buffer);
		}

		/**
		 * When an object is returned to the pool, clear the buffer.
		 */
		@Override
		public void passivateObject(PooledObject<HbRender> pooledObject) {
			// pooledObject.getObject().setLength(0);
		}

		// for all other methods, the no-op implementation
		// in BasePooledObjectFactory will suffice
	}

}
