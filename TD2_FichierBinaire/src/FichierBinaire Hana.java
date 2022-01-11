package TD2;

import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;

class FichierBinaire {
    class Produit {
        int ref; // une référence
        float prix; // un prix
        int qte;

        public Produit() {
            this.ref = 0;
            this.prix = 0;
            this.qte = 0;
        }

        public Produit(int ref, float prix, int qte) {
            this.ref = ref;
            this.prix = prix;
            this.qte = qte;
        }

        // nombre d'octets pour stocker un produit
        static final int BYTES=Integer.BYTES+Float.BYTES+Integer.BYTES;
    }

    FileChannel f; // le fichier binaire
    ByteBuffer buf; // le tampon pour écrire dans le fichier

    /**
     * écrire un produit à la position courante du fichier
     */
    void ecrireProduit(Produit prod) throws IOException {
        // copier le produit dans le tampon
        buf.clear(); // avant d'écrire, on vide le tampon
        buf.putInt(prod.ref);
        buf.putFloat(prod.prix);
        buf.putInt(prod.qte);
        // copier le tampon dans le fichier
        buf.flip(); // passage à une lecture du tampon
        while(buf.hasRemaining()) // tant qu'on n'a pas écrit tout le buffer
            f.write(buf);
    }


    /**
     * lire un produit à la position courante du fichier
     */
    Produit lireProduit() throws IOException {
        // copie du fichier vers le tampon
        buf.clear(); // avant d'écrire, on vide le tampon
        while(buf.hasRemaining()) // tant qu'on n'a pas rempli le buffer
            if(f.read(buf)==-1)
                return null;
        // copie du tampon vers le produit
        buf.flip(); // passage à une lecture du tampon
        Produit prod=new Produit();
        // il faut relire les données dans le même ordre que lors de l'écriture
        prod.ref=buf.getInt();
        prod.prix=buf.getFloat();
        prod.qte=buf.getInt();
        return prod;
    }

    FichierBinaire(String filename) throws IOException {
        //ouverture en lecture/écriture, avec création du fichier
        f=FileChannel.open(
                FileSystems.getDefault().getPath(filename),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE);
        // création d'un buffer juste assez grand pour contenir un produit
        buf=ByteBuffer.allocate(Produit.BYTES);
    }

    /**
     * création du fichier
     */
    void ecrire() throws IOException {
        f.truncate(0); //supprime tous les éléments que contient le fichier
        Produit prod=new Produit();
        for(int id=1;id<=5;id++) {
            prod.ref=id;
            prod.prix=id*10;
            prod.qte=(int) Math.pow(id,2)+15;
            ecrireProduit(prod);
        }
    }

    /**
     * relecture du fichier
     */
    void lire() throws IOException {
        Produit prod;
        f.position(0); // revenir au début du fichier
        while((prod=lireProduit())!=null)
            System.out.println("produit numéro: "+prod.ref+"\tprix: "+prod.prix+"\tquantité: "+prod.qte);
    }


    /**
     * affiche le un produit à l'aide de son id
     * @param id
     * @throws IOException
     */
    void lireId(int id) throws IOException {
        Produit prod;
        f.position(0); // revenir au début du fichier
        boolean trouve = false;
        while((prod=lireProduit())!=null && !trouve)
            if (prod.ref==id){
                System.out.println("produit numéro: "+prod.ref+"\t"+"prix: "+prod.prix+"\tquantité: "+prod.qte);
                trouve=true;
            }
    }

    /**
     * affiche le produit à la postion donné en paramètre.
     * @param pos
     * @throws IOException
     */
    void lirePos(int pos) throws IOException {
        if (pos<=0 || pos>f.size()/Produit.BYTES){
            System.out.println("Position erroné");
            return;
        }
        Produit prod;
        f.position(Produit.BYTES*(pos-1));
        if((prod=lireProduit())!=null){
            System.out.println("produit numéro: "+prod.ref+"\t"+"prix: "+prod.prix+"\tquantité: "+prod.qte);
        }
    }

    /**
     * relecture du fichier à l'envers
     */
    void lireALEnvers() throws IOException {
        Produit prod;
        long pos=f.size()-Produit.BYTES; // position du dernier produit

        while(pos>=0) {
            f.position(pos);
            prod=lireProduit();
            System.out.println("produit numéro: "+prod.ref+"\t"+"prix: "+prod.prix+"\tquantité: "+prod.qte);
            pos-=Produit.BYTES;
        }
    }

    /**
     * Ajoute un produit dans le fichier s'il son id n'existe pas
     * Si le produit existe déjà, cela mets à jour le produit et affiche un message l'indiquant.
     * @param produit
     * @throws IOException
     */
    void ajoutProduit(Produit produit) throws IOException{
        int pos;
        if ((pos= cherchePos(produit.ref))==-1){ // si le produit n'est pas présent
            f.position(f.size());
            ecrireProduit(produit);
        }else{
            f.position(pos*Produit.BYTES);
            ecrireProduit(produit);
            System.out.println("Mise à jour du produit numéro: "+produit.ref);
        }
    }

    void variationQte(int id, int qte) throws IOException{
        int index = cherchePos(id);
        if (index==-1){
            System.out.println("Le produit n'existe pas");

            String reponse="";
            Scanner in = new Scanner(System.in);
            do {
                System.out.println("Voulez-vous creer un nouveau produit (\"o\" pour oui / \"n\" pour non)");
                reponse=in.next();
            }while (!reponse.equals("o") && !reponse.equals("n"));
            if (reponse.equals("o")) {
                float rep=-1;
                do {
                    System.out.println("Quel prix pour ce produit ?");
                    rep=in.nextFloat();
                }while (rep<0);
                f.position(f.size());
                ecrireProduit(new Produit(id,rep,qte));
            }
            return;
        }
        f.position(index*Produit.BYTES);
        Produit prod = lireProduit();
        prod.qte+=qte;
        if (prod.qte<0){
            System.out.println("La quantité est inférieur ne peut être < 0");
            prod.qte=0;
        }

        f.position(index*Produit.BYTES);
        ecrireProduit(prod);
    }

    /**
     *
     * @param id
     * @return la position où se situe le produit dont l'id est donne en paramètre
     * retourne -1 s'il n'est pas présent
     * @throws IOException
     */
    int cherchePos(int id) throws IOException {
        Produit prod;
        int posProd=-1;
        f.position(0); // revenir au début du fichier
        boolean trouve = false;
        while((prod=lireProduit())!=null && !trouve) {
            posProd++;
            if (prod.ref == id) {
                trouve = true;
            }
        }
        if (trouve) return posProd;
        else return -1;
    }

    /**
     * Delete le produit dont l'identifiant est donné en paramètre
     * @param id
     * @throws IOException
     */
    void deleteId(int id) throws IOException{
        int pos= cherchePos(id);
        if (pos==-1) System.out.println("Ce produit n'xiste pas");
        else {
            f.position(f.size()-Produit.BYTES);
            Produit p = lireProduit();
            f.position((long) pos *Produit.BYTES);
            ecrireProduit(p);
            f.truncate(f.size()-Produit.BYTES);
        }
    }


    void run() throws IOException {
        ecrire();
        lire();
        System.out.println("---------");
        lireALEnvers();
        System.out.println("--------");
        lireId(4);
        System.out.println("---------");
        lirePos(6);
        System.out.println("---------");
        ajoutProduit(new Produit(9,(float)25.5,3));
        ajoutProduit(new Produit(3,(float)25.5,3));
        lire();
        System.out.println("-------------");
        deleteId(4);
        lire();
        System.out.println("---------------------");
        variationQte(9,-200);
        variationQte(6, 250);
        lire();
        f.close();
    }

    public static void main(String[] args) {
        try {
            FichierBinaire bin=new FichierBinaire("src/TD2/catalogue.bin");
            bin.run();
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}