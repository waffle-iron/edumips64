/*
 * CodeParsingAlgorithm.java
 *
 * Parsing algorithm associated to the .code section.
 *
 * (c) 2008 Andrea Spadaccini
 *
 * This file is part of the EduMIPS64 project, and is released under the GNU
 * General Public License.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edumips64.core.parser;

import edumips64.core.parser.tokens.*;
import edumips64.core.is.*;
import edumips64.core.*;
import java.util.*;

class CodeParsingAlgorithm extends ParsingAlgorithm {

    public CodeParsingAlgorithm(Parser p) {
        super(p);
    }

    public void parse(Scanner s) {
        edumips64.Main.logger.debug("Starting CodeParsingAlgorithm");
        int address = 0;
        while(s.hasToken()) {
            edumips64.Main.logger.debug("Starting Instruction parsing cycle");
            String label = null;
            Token token = s.next();
            Token instructionToken;
            String data = token.getBuffer();

            // Error
            if(token.isErrorToken()) {
                parser.addError(token, "Lexical error");
                continue;
            }


            // Directive
            else if(token.validate('D')) {
                if(parser.hasAlgorithm(data)) {
                    parser.switchParsingAlgorithm(data);
                    break;
                }
                else {
                    parser.addError(token, "Invalid directive");
                    continue;
                } 
            }

            // ID
            else if(token.validate('L')) {
                // Label?
                Instruction tmpInstr = Instruction.buildInstruction(data);
                instructionToken = token;
                if(tmpInstr == null) {
                    // Label! It would be useful if there was a "EndOfTokenStream" exception
                    token = s.next(); 
                    if(!token.validate(':')) {
                        parser.addError(token, "Colon expected");
                        continue;
                    } 
                    else {
                        // Good label
                        label = data;
                        token = s.next();
                        tmpInstr = Instruction.buildInstruction(token.getBuffer());
                        instructionToken = token;
                        if(tmpInstr == null) {
                            parser.addError(token, "Instruction expected");
                            continue;
                        }
                    }
                }

                // Now we should have a valid instruction. Let's get its
                // syntax and validate the remaining tokens against it.
                String syntax = tmpInstr.getSyntax();
                int syntax_length = syntax.length();
                String fullname = instructionToken.getBuffer() + " ";
                List<Token> params = new LinkedList<Token>();

                for(int i = 0; i < syntax_length; ++i) {
                    char c = syntax.charAt(i);

                    // The syntax string is something like "%I,%I,%C".
                    // We don't need the % characters, so we strip them out.
                    if(c == '%')
                        continue;
                    token = s.next();
                    if(!token.validate(c))
                        parser.addError(token, "Unexpected token");
                    else {
                        params.add(token);
                        fullname += token.getBuffer();
                        edumips64.Main.logger.debug(fullname);
                    }
                }

                tmpInstr.setFullName(fullname);
                edumips64.Main.logger.debug("Added " + fullname);

                parser.addInstruction(tmpInstr, address, params, label, instructionToken);


                token = s.next();
                if(!token.validate('\n'))
                    parser.addError(token, "Expected EOL");

                address += 4;
            }
        }
    }
}