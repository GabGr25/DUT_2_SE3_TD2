import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;

class FichierBinaire {
    class Produit {
        int quantite;
        int ref; // une référence
        float prix; // un prix

        // nombre d'octets pour stocker un produit
        static final int BYTES=2*Integer.BYTES+Float.BYTES;

        public Produit(int quantite, int ref, float prix) {
            this.quantite = quantite;
            this.ref = ref;
            this.prix = prix;
        }
    };

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
        buf.putInt(prod.quantite);
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
        Produit prod=new Produit(4,8,9);
        // il faut relire les données dans le même ordre que lors de l'écriture
        prod.ref=buf.getInt();
        prod.prix=buf.getFloat();
        prod.quantite=buf.getInt();
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
    public void ecrire(int nb) throws IOException {
        Random r = new Random();
        Produit prod = new Produit(5,7,9);
        System.out.println();
        f.truncate(0);
        for(int i = 0; i<nb; i++){
            prod.ref = i;
            prod.prix = r.nextFloat() / (float) 100;
            prod.quantite = r.nextInt(100000);
            ecrireProduit(prod);
        }

    }

    /**
     * relecture du fichier
     */
    void lire() throws IOException {
        int nbElement=((int)f.size())/Produit.BYTES;
        System.out.println("\t lecture du fichier (nb="+nbElement+") :");

        Produit prod;
        f.position(0); // revenir au début du fichier

        while((prod=lireProduit())!=null)
            System.out.println(prod.ref+"\t"+prod.prix+"\t"+prod.quantite);
            prod=lireProduit();
        }

    String getProduit(int n) throws IOException{
        int nbElements = ((int) f.size())/Produit.BYTES;
        System.out.println("\t Lecture du fichier (nb="+nbElements+") :");

        Produit prod;
        f.position(0);
        while((prod=lireProduit())!=null){
            System.out.println(prod.ref+"\t"+prod.prix+"\t"+prod.quantite);
        }
        return null;
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
            System.out.println(prod.ref+"\t"+prod.prix);
            pos-=Produit.BYTES;
        }
    }

    int rechercherIndexProduit(int ref) throws IOException{
        int index=1;
        Produit prod;
        f.position(0);//revenir au debut du fichier
        while((prod=lireProduit())!=null){
            if(prod.ref==ref) return index;
            index ++;
        }
        return -1;
    }

    void ajouterProduit(Produit p) throws IOException{
        Produit prod;
        int index=rechercherIndexProduit(p.ref);
        if(index==-1){
            f.position(f.size());
            ecrireProduit(p);
            System.out.println("\n Nouveau produit \n");
        }
        else{
            f.position((index-1)*Produit.BYTES);
            ecrireProduit(p);
            System.out.println("Produit existe \n");
        }
    }

    void changementQuantite(int ref, int quantite)throws IOException{
        int index=rechercherIndexProduit(ref);
        if(index==-1)return;
        f.position((index-1)*Produit.BYTES);
        Produit prod=lireProduit();
        if(prod.quantite+quantite>=0)
            prod.quantite+=quantite;
        f.position((index-1)*Produit.BYTES);
        ecrireProduit(prod);
    }

    void deleteProduit(int ref) throws IOException{
        Produit prod;
        int index=rechercherIndexProduit(ref);
        if(index==-1) System.out.println("Fichier existe pas");
        else{
            f.position(f.size()-Produit.BYTES);
            prod=lireProduit();
            f.position((index-1)*Produit.BYTES);
            ecrireProduit(prod);
            f.truncate(f.size()-Produit.BYTES);
            System.out.println("Produit existe plus \n");
        }
    }


    void run() throws IOException{
        ecrire(3);
        lire();
        Produit m=new Produit(4,9,10);
        Produit p =new Produit(2,18,15);
        ajouterProduit(m);
        ajouterProduit(p);
        deleteProduit(2);
        changementQuantite(9,-8);
        lire();
        f.close();
    }

    public static void main(String[] args) {
        try {
            FichierBinaire bin=new FichierBinaire("/Users/gab/Desktop/Cours/2nd année/SE3/TD2/catalogue.bin"
            );
            bin.run();
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}