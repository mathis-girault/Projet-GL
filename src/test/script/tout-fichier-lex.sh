
#Un script shell qui lance tous les tests créer pour le lexeur

#Utilisation: On créer des fihiers de tests dans src/test/deca/syntax/[invalid|valid] avec comme nom "fichier_test.deca"
#Dans le dossier src/test/deca/syntax/[invalid|valid]/resultat on met pour chaque fichier test "fichier_test.deca"
#un fichier resultat "fichier_test-resultat.txt" qui contient le résultat attendu par le lexeur.

#Ce script comparera le résultat du lexeur et le fichier résultat, écrira ok si se sont les mêmes et faux sinon


cd "$(dirname "$0")"/../../.. || exit 1

PATH=./src/test/script/launchers:"$PATH"
echo "Début des tests valid"
echo ""
echo "---------------------------------------"
echo ""

for fichier in ./src/test/deca/syntax/valid/homemade/lexer/test/*.deca
do
    nom=${fichier##*/}
    export nom
    test_lex "$fichier" 2>&1 > actual
    if ! diff -Z actual "./src/test/deca/syntax/valid/homemade/lexer/resultat/${nom%.deca}_resultat.txt"
    then
        echo ""
        echo "$fichier"
        echo "faux"
    fi
done

echo ""
echo "Début des tests invalids"
echo "--------------------------------------"
echo ""

for fichier in ./src/test/deca/syntax/invalid/homemade/lexer/test/*.deca
do
    nom=${fichier##*/}
    export nom
    t=$(test_lex "$fichier" 2>&1 >actual)
    if ! diff -Z actual "./src/test/deca/syntax/invalid/homemade/lexer/resultat/${nom%.deca}_resultat.txt"
    then
        echo ""
        echo "$fichier"
        echo "faux"
    fi
done

rm actual