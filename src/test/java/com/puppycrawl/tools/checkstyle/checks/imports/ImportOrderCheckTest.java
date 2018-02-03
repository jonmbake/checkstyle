////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2018 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.checks.imports;

import static com.puppycrawl.tools.checkstyle.checks.imports.ImportOrderCheck.MSG_ORDERING;
import static com.puppycrawl.tools.checkstyle.checks.imports.ImportOrderCheck.MSG_SEPARATED_IN_GROUP;
import static com.puppycrawl.tools.checkstyle.checks.imports.ImportOrderCheck.MSG_SEPARATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import antlr.CommonHiddenStreamToken;
import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ImportOrderOption.class)
public class ImportOrderCheckTest extends AbstractModuleTestSupport {

    @Override
    protected String getPackageLocation() {
        return "com/puppycrawl/tools/checkstyle/checks/imports/importorder";
    }

    /* Additional test for jacoco, since valueOf()
     * is generated by javac and jacoco reports that
     * valueOf() is uncovered.
     */
    @Test
    public void testImportOrderOptionValueOf() {
        final ImportOrderOption option = ImportOrderOption.valueOf("TOP");
        assertEquals("Invalid valueOf result", ImportOrderOption.TOP, option);
    }

    @Test
    public void testDefault() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        final String[] expected = {
            "5: " + getCheckMessage(MSG_ORDERING, "java.awt.Dialog"),
            "9: " + getCheckMessage(MSG_ORDERING, "javax.swing.JComponent"),
            "11: " + getCheckMessage(MSG_ORDERING, "java.io.File"),
            "13: " + getCheckMessage(MSG_ORDERING, "java.io.IOException"),
        };

        verify(checkConfig, getPath("InputImportOrder.java"), expected);
    }

    @Test
    public void testGroups() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "java.awt");
        checkConfig.addAttribute("groups", "javax.swing");
        checkConfig.addAttribute("groups", "java.io");
        final String[] expected = {
            "5: " + getCheckMessage(MSG_ORDERING, "java.awt.Dialog"),
            "13: " + getCheckMessage(MSG_ORDERING, "java.io.IOException"),
            "16: " + getCheckMessage(MSG_ORDERING, "javax.swing.WindowConstants.*"),
        };

        verify(checkConfig, getPath("InputImportOrder.java"), expected);
    }

    @Test
    public void testGroupsRegexp() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "java, /^javax?\\.(awt|swing)\\./");
        checkConfig.addAttribute("ordered", "false");
        final String[] expected = {
            "11: " + getCheckMessage(MSG_ORDERING, "java.io.File"),
        };

        verify(checkConfig, getPath("InputImportOrder.java"), expected);
    }

    @Test
    public void testSeparated() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "java.awt, javax.swing, java.io, java.util");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("ordered", "false");
        final String[] expected = {
            "9: " + getCheckMessage(MSG_SEPARATION, "javax.swing.JComponent"),
            "11: " + getCheckMessage(MSG_SEPARATION, "java.io.File"),
            "16: " + getCheckMessage(MSG_ORDERING, "javax.swing.WindowConstants.*"),
        };

        verify(checkConfig, getPath("InputImportOrder.java"), expected);
    }

    @Test
    public void testStaticImportSeparated() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "java, org");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("ordered", "true");
        checkConfig.addAttribute("option", "top");
        final String[] expected = {
            "5: " + getCheckMessage(MSG_SEPARATED_IN_GROUP, "java.lang.Math.cos"),
            "7: " + getCheckMessage(MSG_SEPARATED_IN_GROUP, "org.junit.Assert.assertEquals"),
        };

        verify(checkConfig, getPath("InputImportOrderStaticGroupSeparated.java"), expected);
    }

    @Test
    public void testNoGapBetweenStaticImports() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "java, javax, org");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("ordered", "true");
        checkConfig.addAttribute("option", "bottom");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

        verify(checkConfig, getPath("InputImportOrderNoGapBetweenStaticImports.java"), expected);
    }

    @Test
    public void testSortStaticImportsAlphabeticallyFalse() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", " java, javax, org");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("ordered", "true");
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "false");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

        verify(checkConfig, getPath("InputImportOrderSortStaticImportsAlphabetically.java"),
            expected);
    }

    @Test
    public void testSortStaticImportsAlphabeticallyTrue() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "java, javax, org");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("ordered", "true");
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = {
            "4: " + getCheckMessage(MSG_ORDERING,
                "javax.xml.transform.TransformerFactory.newInstance"),
            "5: " + getCheckMessage(MSG_ORDERING, "java.lang.Math.cos"),
            "6: " + getCheckMessage(MSG_ORDERING, "java.lang.Math.abs"),
        };

        verify(checkConfig, getPath("InputImportOrderSortStaticImportsAlphabetically.java"),
            expected);
    }

    @Test
    public void testCaseInsensitive() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("caseSensitive", "false");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

        verify(checkConfig, getPath("InputImportOrderCaseInsensitive.java"), expected);
    }

    @Test
    public void testContainerCaseInsensitive() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("caseSensitive", "false");
        checkConfig.addAttribute("useContainerOrderingForStatic", "true");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

        verify(checkConfig, getNonCompilablePath("InputImportOrderEclipseStaticCaseSensitive.java"),
            expected);
    }

    @Test
    public void testInvalidOption() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "invalid_option");

        try {
            final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

            verify(checkConfig, getPath("InputImportOrder_Top.java"), expected);
            fail("exception expected");
        }
        catch (CheckstyleException ex) {
            final String messageStart = "cannot initialize module "
                + "com.puppycrawl.tools.checkstyle.TreeWalker - Cannot set property 'option' to "
                + "'invalid_option' in module";

            assertTrue("Invalid exception message, should start with: " + messageStart,
                ex.getMessage().startsWith(messageStart));
        }
    }

    @Test
    public void testTop() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "top");
        final String[] expected = {
            "18: " + getCheckMessage(MSG_ORDERING, "java.io.File.*"),
        };

        verify(checkConfig, getPath("InputImportOrder_Top.java"), expected);
    }

    @Test
    public void testAbove() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "above");
        final String[] expected = {
            "5: " + getCheckMessage(MSG_ORDERING, "java.awt.Button.ABORT"),
            "8: " + getCheckMessage(MSG_ORDERING, "java.awt.Dialog"),
            "13: " + getCheckMessage(MSG_ORDERING, "java.io.File"),
            "14: " + getCheckMessage(MSG_ORDERING, "java.io.File.createTempFile"),
        };

        verify(checkConfig, getPath("InputImportOrder_Above.java"), expected);
    }

    @Test
    public void testInFlow() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "inflow");
        final String[] expected = {
            "6: " + getCheckMessage(MSG_ORDERING, "java.awt.Dialog"),
            "11: " + getCheckMessage(MSG_ORDERING,
                     "javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE"),
            "12: " + getCheckMessage(MSG_ORDERING, "javax.swing.WindowConstants.*"),
            "13: " + getCheckMessage(MSG_ORDERING, "javax.swing.JTable"),
            "15: " + getCheckMessage(MSG_ORDERING, "java.io.File.createTempFile"),
            "16: " + getCheckMessage(MSG_ORDERING, "java.io.File"),
        };

        verify(checkConfig, getPath("InputImportOrder_InFlow.java"), expected);
    }

    @Test
    public void testUnder() throws Exception {
        // is default (testDefault)
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "under");
        final String[] expected = {
            "5: " + getCheckMessage(MSG_ORDERING, "java.awt.Dialog"),
            "11: " + getCheckMessage(MSG_ORDERING, "java.awt.Button.ABORT"),
            "14: " + getCheckMessage(MSG_ORDERING, "java.io.File"),
        };

        verify(checkConfig, getPath("InputImportOrder_Under.java"), expected);
    }

    @Test
    public void testBottom() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "bottom");
        final String[] expected = {
            "15: " + getCheckMessage(MSG_ORDERING, "java.io.File"),
            "21: " + getCheckMessage(MSG_ORDERING, "java.io.Reader"),
        };

        verify(checkConfig, getPath("InputImportOrder_Bottom.java"), expected);
    }

    @Test
    public void testGetGroupNumber() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "/javax/, sun, /^java/, org, /java/");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

        verify(checkConfig,
            getNonCompilablePath("InputImportOrderGetGroupNumber.java"), expected);
    }

    @Test
    public void testHonorsTokenProperty() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("tokens", "IMPORT");
        final String[] expected = {
            "6: " + getCheckMessage(MSG_ORDERING, "java.awt.Button"),
        };

        verify(checkConfig, getPath("InputImportOrder_HonorsTokensProperty.java"), expected);
    }

    @Test
    public void testWildcard() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "com,*,java");
        final String[] expected = {
            "9: " + getCheckMessage(MSG_ORDERING, "javax.crypto.Cipher"),
        };

        verify(checkConfig, getPath("InputImportOrder_Wildcard.java"), expected);
    }

    @Test
    public void testWildcardUnspecified() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);

        checkConfig.addAttribute("groups", "java,javax,org");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

        verify(checkConfig, getPath("InputImportOrder_WildcardUnspecified.java"), expected);
    }

    @Test
    public void testNoFailureForRedundantImports() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getPath("InputImportOrder_NoFailureForRedundantImports.java"),
            expected);
    }

    @Test
    public void testStaticGroupsAlphabeticalOrder() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("groups", "org, java");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getPath("InputImportOrderStaticGroupOrder.java"), expected);
    }

    @Test
    public void testStaticGroupsOrder() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("groups", "org, java");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getPath("InputImportOrderStaticGroupOrder.java"), expected);
    }

    @Test
    public void testStaticGroupsAlphabeticalOrderBottom() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "bottom");
        checkConfig.addAttribute("groups", "org, java");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getPath("InputImportOrderStaticGroupOrderBottom.java"), expected);
    }

    @Test
    public void testStaticGroupsAlphabeticalOrderBottomNegative() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "bottom");
        checkConfig.addAttribute("groups", "org, java");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = {
            "8: " + getCheckMessage(MSG_ORDERING, "java.util.Set"),
        };
        verify(checkConfig, getPath("InputImportOrderStaticGroupOrderBottom_Negative.java"),
            expected);
    }

    /** Tests that a non-static import after a static import correctly gives an
     * error if order=bottom. */

    @Test
    public void testStaticGroupsAlphabeticalOrderTopNegative() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("groups", "org, java");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = {
            "5: " + getCheckMessage(MSG_ORDERING, "java.lang.Math.PI"),
        };
        verify(checkConfig, getPath("InputImportOrderStaticGroupOrderBottom_Negative.java"),
            expected);
    }

    /** Tests that a non-static import before a static import correctly gives an
     * error if order=top. */

    @Test
    public void testStaticGroupsAlphabeticalOrderBottomNegative2() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "bottom");
        checkConfig.addAttribute("groups", "org, java");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = {
            "8: " + getCheckMessage(MSG_ORDERING, "java.util.Set"),
        };
        verify(checkConfig, getPath("InputImportOrderStaticGroupOrderBottom_Negative2.java"),
            expected);
    }

    @Test
    public void testStaticGroupsOrderBottom() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "bottom");
        checkConfig.addAttribute("groups", "org, java");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getPath("InputImportOrderStaticGroupOrderBottom.java"), expected);
    }

    @Test
    public void testImportReception() throws Exception {
        final DefaultConfiguration checkConfig =
                createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("groups", "java, javax");
        final String[] expected = {
            "6: " + getCheckMessage(MSG_ORDERING, "java.awt.event.ActionEvent"),
        };
        verify(checkConfig, getPath("InputImportOrderRepetition.java"), expected);
    }

    @Test
    public void testStaticImportReceptionTop() throws Exception {
        final DefaultConfiguration checkConfig =
                createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("groups", "java, org");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = {
            "5: " + getCheckMessage(MSG_ORDERING, "org.antlr.v4.runtime.CommonToken.*"),
        };
        verify(checkConfig, getPath("InputImportOrderStaticRepetition.java"), expected);
    }

    @Test
    public void testStaticImportReception() throws Exception {
        final DefaultConfiguration checkConfig =
                createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("groups", "java, org");
        final String[] expected = {
            "4: " + getCheckMessage(MSG_SEPARATION, "org.antlr.v4.runtime.CommonToken.*"),
            "5: " + getCheckMessage(MSG_ORDERING, "org.antlr.v4.runtime.CommonToken.*"),
            "7: " + getCheckMessage(MSG_ORDERING, "java.util.Set"),
        };
        verify(checkConfig, getPath("InputImportOrderStaticRepetition.java"), expected);
    }

    @Test
    public void testStaticGroupsOrderAbove() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "above");
        checkConfig.addAttribute("groups", "org, java, sun");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = {
            "7: " + getCheckMessage(MSG_ORDERING, "java.lang.Math.PI"),
            "8: " + getCheckMessage(MSG_ORDERING, "org.antlr.v4.runtime.Recognizer.EOF"),
        };
        verify(checkConfig, getPath("InputImportOrderStaticGroupOrderBottom.java"), expected);
    }

    @Test
    public void testStaticOnDemandGroupsOrder() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("groups", "org, java");
        final String[] expected = {
            "9: " + getCheckMessage(MSG_ORDERING, "org.junit.Test"),
        };
        verify(checkConfig, getPath("InputImportOrderStaticOnDemandGroupOrder.java"), expected);
    }

    @Test
    public void testStaticOnDemandGroupsAlphabeticalOrder() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("groups", "org, java");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = {
            "9: " + getCheckMessage(MSG_ORDERING, "org.junit.Test"),
        };
        verify(checkConfig, getPath("InputImportOrderStaticOnDemandGroupOrder.java"), expected);
    }

    @Test
    public void testStaticOnDemandGroupsOrderBottom() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "bottom");
        checkConfig.addAttribute("groups", "org, java");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getPath("InputImportOrderStaticOnDemandGroupOrderBottom.java"),
            expected);
    }

    @Test
    public void testStaticOnDemandGroupsAlphabeticalOrderBottom() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "bottom");
        checkConfig.addAttribute("groups", "org, java");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getPath("InputImportOrderStaticOnDemandGroupOrderBottom.java"),
            expected);
    }

    @Test
    public void testStaticOnDemandGroupsOrderAbove() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "above");
        checkConfig.addAttribute("groups", "org, java");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = {
            "7: " + getCheckMessage(MSG_ORDERING, "java.lang.Math.*"),
            "8: " + getCheckMessage(MSG_ORDERING, "org.antlr.v4.runtime.CommonToken.*"),
        };
        verify(checkConfig, getPath("InputImportOrderStaticOnDemandGroupOrderBottom.java"),
            expected);
    }

    @Test
    public void testGroupWithSlashes() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "/^javax");

        try {
            final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

            verify(checkConfig, getPath("InputImportOrder.java"), expected);
            fail("exception expected");
        }
        catch (CheckstyleException ex) {
            final String messageStart = "cannot initialize module "
                + "com.puppycrawl.tools.checkstyle.TreeWalker - Cannot set property"
                + " 'groups' to '/^javax' in module";

            assertTrue("Invalid exception message, should start with: " + messageStart,
                ex.getMessage().startsWith(messageStart));
        }
    }

    @Test
    public void testGroupWithDot() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "java.awt.");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

        verify(checkConfig, getPath("InputImportOrder_NoFailureForRedundantImports.java"),
            expected);
    }

    @Test
    public void testMultiplePatternMatches() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "/java/,/rga/,/myO/,/org/,/organ./");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getNonCompilablePath("InputImportOrder_MultiplePatternMatches.java"),
            expected);
    }

    // -@cs[ForbidAnnotationElementValue] Will examine turkish failure
    @Test(expected = IllegalStateException.class)
    public void testVisitTokenSwitchReflection() {
        // Create mock ast
        final DetailAST astImport = mockAST(TokenTypes.IMPORT, "import", "mockfile", 0, 0);
        final DetailAST astIdent = mockAST(TokenTypes.IDENT, "myTestImport", "mockfile", 0, 0);
        astImport.addChild(astIdent);
        final DetailAST astSemi = mockAST(TokenTypes.SEMI, ";", "mockfile", 0, 0);
        astIdent.addNextSibling(astSemi);

        // Set unsupported option
        final ImportOrderCheck mock = new ImportOrderCheck();
        final ImportOrderOption importOrderOptionMock = PowerMockito.mock(ImportOrderOption.class);
        Whitebox.setInternalState(importOrderOptionMock, "name", "NEW_OPTION_FOR_UT");
        Whitebox.setInternalState(importOrderOptionMock, "ordinal", 5);
        Whitebox.setInternalState(mock, "option", importOrderOptionMock);

        // expecting IllegalStateException
        mock.visitToken(astImport);
    }

    /**
     * Creates MOCK lexical token and returns AST node for this token.
     * @param tokenType type of token
     * @param tokenText text of token
     * @param tokenFileName file name of token
     * @param tokenRow token position in a file (row)
     * @param tokenColumn token position in a file (column)
     * @return AST node for the token
     */
    private static DetailAST mockAST(final int tokenType, final String tokenText,
            final String tokenFileName, final int tokenRow, final int tokenColumn) {
        final CommonHiddenStreamToken tokenImportSemi = new CommonHiddenStreamToken();
        tokenImportSemi.setType(tokenType);
        tokenImportSemi.setText(tokenText);
        tokenImportSemi.setLine(tokenRow);
        tokenImportSemi.setColumn(tokenColumn);
        tokenImportSemi.setFilename(tokenFileName);
        final DetailAST astSemi = new DetailAST();
        astSemi.initialize(tokenImportSemi);
        return astSemi;
    }

    @Test
    public void testEclipseDefaultPositive() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "java,javax,org,com");
        checkConfig.addAttribute("ordered", "true");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;

        verify(checkConfig, getPath("InputImportOrder_EclipseDefaultPositive.java"), expected);
    }

    @Test
    public void testStaticImportEclipseRepetition() throws Exception {
        final DefaultConfiguration checkConfig =
            createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("groups", "java, org");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        checkConfig.addAttribute("useContainerOrderingForStatic", "true");
        final String[] expected = {
            "4: " + getCheckMessage(MSG_ORDERING,
                "io.netty.handler.codec.http.HttpHeaders.Names.DATE"),
        };
        verify(checkConfig,
            getNonCompilablePath("InputImportOrderEclipseStaticRepetition.java"), expected);
    }

    @Test
    public void testEclipseDefaultNegative() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "java,javax,org,com");
        checkConfig.addAttribute("ordered", "true");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        final String[] expected = {
            "12: " + getCheckMessage(MSG_SEPARATION, "javax.swing.JComponent"),
            "17: " + getCheckMessage(MSG_ORDERING, "org.junit.Test"),
            };

        verify(checkConfig, getPath("InputImportOrder_EclipseDefaultNegative.java"), expected);
    }

    @Test
    public void testUseContainerOrderingForStaticTrue() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "/^javax?\\./,org");
        checkConfig.addAttribute("ordered", "true");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("caseSensitive", "false");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        checkConfig.addAttribute("useContainerOrderingForStatic", "true");
        final String[] expected = CommonUtils.EMPTY_STRING_ARRAY;
        verify(checkConfig, getNonCompilablePath("InputImportOrderEclipseStatic.java"), expected);
    }

    @Test
    public void testUseContainerOrderingForStaticFalse() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "/^javax?\\./,org");
        checkConfig.addAttribute("ordered", "true");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("caseSensitive", "false");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        checkConfig.addAttribute("useContainerOrderingForStatic", "false");
        final String[] expected = {
            "6: " + getCheckMessage(MSG_ORDERING,
                "io.netty.handler.codec.http.HttpHeaders.Names.addDate"),
        };
        verify(checkConfig, getNonCompilablePath("InputImportOrderEclipseStatic.java"), expected);
    }

    @Test
    public void testUseContainerOrderingForStaticTrueCaseSensitive() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "/^javax?\\./,org");
        checkConfig.addAttribute("ordered", "true");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("option", "top");
        checkConfig.addAttribute("sortStaticImportsAlphabetically", "true");
        checkConfig.addAttribute("useContainerOrderingForStatic", "true");
        final String[] expected = {
            "7: " + getCheckMessage(MSG_ORDERING,
                "io.netty.handler.codec.http.HttpHeaders.Names.DATE"),
            };
        verify(checkConfig, getNonCompilablePath("InputImportOrderEclipseStatic.java"), expected);
    }

    @Test
    public void testImportGroupsRedundantSeparatedInternally() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(ImportOrderCheck.class);
        checkConfig.addAttribute("groups", "/^javax\\./,com");
        checkConfig.addAttribute("ordered", "true");
        checkConfig.addAttribute("separated", "true");
        checkConfig.addAttribute("option", "bottom");
        final String[] expected = {
            "5: " + getCheckMessage(MSG_SEPARATED_IN_GROUP, "org.*"),
        };
        verify(checkConfig, getNonCompilablePath("InputImportOrder_MultiplePatternMatches.java"),
                expected);
    }

}
