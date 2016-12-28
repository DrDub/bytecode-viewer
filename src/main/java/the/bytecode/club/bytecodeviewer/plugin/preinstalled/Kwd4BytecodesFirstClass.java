package the.bytecode.club.bytecodeviewer.plugin.preinstalled;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.tuple.Pair;
import org.keywords4bytecodes.firstclass.FirstClassSystem;
import org.keywords4bytecodes.firstclass.Predictions;
import org.keywords4bytecodes.firstclass.RandomForestSystem;
import org.keywords4bytecodes.firstclass.TermVocabulary;
import org.keywords4bytecodes.firstclass.extract.BytecodeData;
import org.keywords4bytecodes.firstclass.extract.BytecodeData.MethodData;
import org.keywords4bytecodes.firstclass.extract.BytecodeToSequence;
import org.objectweb.asm.tree.ClassNode;

import the.bytecode.club.bytecodeviewer.api.ExceptionUI;
import the.bytecode.club.bytecodeviewer.api.Plugin;
import the.bytecode.club.bytecodeviewer.api.PluginConsole;

/***************************************************************************
 * Bytecode Viewer (BCV) - Java & Android Reverse Engineering Suite        *
 * Copyright (C) 2014 Kalen 'Konloch' Kinloch - http://bytecodeviewer.com  *
 * *
 * This program is free software: you can redistribute it and/or modify    *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 * *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 * *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 ***************************************************************************/

/**
 * Shows the predicted first term for obfuscated methods using Keywords4Bytecodes 1st class.
 *
 * @author Pablo Duboue
 */
public class Kwd4BytecodesFirstClass extends Plugin {

    private FirstClassSystem system;

    public Kwd4BytecodesFirstClass() {
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new GZIPInputStream(new FileInputStream("/tmp/apache_eclipse_float.ser.gz")));
            this.system = (FirstClassSystem) ois.readObject();
            System.out.println("Read system " + system.getClass().getName());
            if (system instanceof RandomForestSystem) {
                TermVocabulary vocab = system.vocab();
                System.out.println("Number of terms: " + vocab.size());
            }
            ois.close();
        } catch (Exception e) {
            new ExceptionUI(e, "pablo.duboue@gmail.com");
        }
    }

    @Override
    public void execute(final ArrayList<ClassNode> classNodeList) {
        final PluginConsole frame = new PluginConsole("K4B 1st class: predicted first term for obfuscated methods");
        final AtomicBoolean complete = new AtomicBoolean(false);
        final Thread backgroundThread = new Thread() {
            public void run() {
                try {
                    System.out.println("Working with system " + system.getClass().getName());

                    for (ClassNode classNode : classNodeList) {
                        List<BytecodeData> data = BytecodeToSequence.extract(classNode);
                        for (BytecodeData bytecodeData : data) {
                            for (MethodData methodData : bytecodeData.getMethods()) {
                                Map<String, Double> predictions = system.predict(methodData);
                                List<Pair<Double, String>> sorted = Predictions.sortPredictions(predictions);
                                // show top 3, with scores
                                Iterator<Pair<Double, String>> it = sorted.iterator();
                                StringBuilder sb = new StringBuilder();
                                int pos = 0;
                                while (pos < 3 && it.hasNext()) {
                                    Pair<Double, String> p = it.next();
                                    pos++;
                                    sb.append("  ").append(p.getRight()).append(String.format(" %.03f", p.getLeft()));
                                }

                                frame.appendText(String.format("%s.%s %s -> %s", classNode.name, methodData.getName(),
                                        methodData.getDesc(), sb.toString()));
                            }
                        }
                    }
                } catch (Exception e) {
                    new ExceptionUI(e, "pablo.duboue@gmail.com");
                } finally {
                    complete.set(true);
                }
            }
        };
        frame.setVisible(true);
        frame.addWindowListener(new WindowListener() {
            @Override
            public void windowClosing(WindowEvent e) {
                backgroundThread.stop();
                complete.set(true);
            }

            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        });
        backgroundThread.start();
        while (!complete.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                new ExceptionUI(e1, "pablo.duboue@gmail.com");
            }
        }
    }
}
