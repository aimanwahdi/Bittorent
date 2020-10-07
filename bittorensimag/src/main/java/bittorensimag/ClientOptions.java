package bittorensimag;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.lang.RuntimeException;

/**
 * User-specified options influencing the compilation.
 *
 * @author gouloisw 
 * @date 06/10/20
 */
public class ClientOptions {

    private boolean debug = false;
    private boolean info = false;
    private boolean printBanner = false;
    private boolean startedPassingFiles = false;
    private List<File> sourceFiles = new ArrayList<File>();


    public void parseArgs(String[] args) throws RuntimeException {
        for (int i = 0; i < args.length; i++){
            String argument = args[i];
            if (argument.charAt(0) == '-'){
                if (startedPassingFiles){
                    throw new RuntimeException("Cannot pass options after files");
                }
                switch(argument.charAt(1)){
                    //bonus afficher banniere en asci art
                    case 'b':
                        printBanner = true;
                        break;
                    case 'd':
                        debug = true;
                        break;
                    case 'i':
                        info = true;
                        break;
                    default:
                        throw new RuntimeException("This option does not exist for the bittorent client");
                }
            } else { //si l'argument n'a pas de tiret, alors c'est un fichier
                startedPassingFiles = true;
                File f = new File(argument);
                if(!sourceFiles.contains(f)) {
                    sourceFiles.add(new File(argument));
                }
            }
        }
    }

    public boolean getPrintBanner() {
        return printBanner;
    }

    public boolean getDebug(){
        return debug;
    }

    public boolean getInfo(){
        return info;
    }

    public List<File> getSourceFiles() {
        return Collections.unmodifiableList(sourceFiles);
    }

    protected void bannerInTerminal(){
        //TODO
        System.out.println("ASCI ART BANNER");                                                                                    
    }

    protected void displayUsage(){
        //TODO
        System.out.println("This is how you should use Bittorensimag");
    } 
}