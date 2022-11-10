package org.wildfly.cli.util;
import picocli.CommandLine.Help.Ansi;

public class ColouredWriter {
    public static void printlnError(String s) {
        String coloured = Ansi.AUTO.string("@|bold,red " + s + "|@");
        System.out.println(coloured);
    }

    public static void printlnWarning(String s) {
        String coloured = Ansi.AUTO.string("@|bold,yellow " + s + "|@");
        System.out.println(coloured);
    }


    public static void printlnSuccess(String s) {
        String coloured = Ansi.AUTO.string("@|bold,green " + s + "|@");
        System.err.println(coloured);
    }
}
