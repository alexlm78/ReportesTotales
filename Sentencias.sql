-- Nietos
INSERT INTO CTL01 
SELECT A.CONBEX, A.CONBLN, A.CONBRS, A.CONMEX, A.CONMLN, A.CONMRS, A.CONCYC 
  FROM GUAV1.CONTROL A 
 INNER JOIN GUAV1.BLCIFAC B ON ( A.CONCYC=B.CICNUM AND B.CICSTS='F' ) 
 WHERE A.CONBEX=A.CONMEX AND A.CONBLN=A.CONMLN AND A.CONBRS=A.CONBRS  AND A.CONBRS=0 

INSERT INTO CTL02 
SELECT A.CONBEX, A.CONBLN, A.CONBRS, A.CONCYC 
  FROM GUAV1.CONTROL A 
       EXCEPTION JOIN CTL01 B ON ( A.CONBEX=B.CONBEX AND A.CONBLN=B.CONBLN AND A.CONBRS=B.CONBRS ) 
 INNER JOIN GUAV1.BLCIFAC C ON ( A.CONCYC=C.CICNUM AND C.CICSTS='F' ) 
 WHERE A.CONBRS=0 GROUP BY A.CONBEX, A.CONBLN, A.CONBRS, A.CONCYC 
 ORDER BY A.CONCYC, A.CONBEX, A.CONBLN, A.CONBRS
 
INSERT INTO CTL03 
SELECT DIGITS(B.CONBEX)||DIGITS(B.CONBLN) AS TEL1, DIGITS(B.CONMEX)||DIGITS(B.CONMLN) AS TEL2, 
       B.CONCYC AS CICLO, B.CONBEX, B.CONBLN, B.CONMEX, B.CONMLN 
  FROM CTL02 A 
 INNER JOIN GUAV1.CONTROL B ON ( A.CONBEX=B.CONMEX AND A.CONBLN=B.CONMLN AND B.CONMRS=0 )
 
INSERT INTO CTL04 
SELECT * FROM CTL02 A 
EXCEPTION JOIN CTL03 B ON ( A.CONBEX=B.CONMEX AND A.CONBLN=B.CONMLN )

DELETE FROM CTL03 
 WHERE CONBEX=CONMEX AND CONBLN=CONMLN 

INSERT INTO CTL05 
SELECT A.*, B.MSOTOS, B.MSOSO�, B.MSOSDT, B.MSOPST, B.MSOF05, B.MSOCOI, B.MSOUCD 
  FROM CTL03 A 
 INNER JOIN GUAV1.SVORD B ON ( DECIMAL(A.TEL2) = B.MSOPH� ) 
 WHERE B.MSOSTS<>'D' AND B.MSOTOS IN('N1','N3','D1','D8','B1','B2') 
   AND B.MSOSDT >=20080101

INSERT INTO CTL06 
SELECT TEL1, TEL2, CAST(CICLO AS INTEGER) AS CICLO, MSOTOS, MSOSO�, CHAR(MSOSDT), CHAR(MSOPST), MSOF05, MSOCOI, MSOUCD 
FROM CTL05

INSERT INTO CTL07 
SELECT A.TEL1, A.TEL2, CAST(A.CICLO AS INTEGER) AS CICLO
FROM CTL03 A 
EXCEPTION JOIN CTL05 B ON ( A.TEL2=B.TEL2 )

INSERT INTO CTL08 
SELECT INTEGER(TEL1) AS PADRE , INTEGER(TEL2) AS HIJO, 
       INTEGER( DIGITS(B.CONMEX)||DIGITS(B.CONMLN) ) AS NIETO, A.CICLO, A.MSOTOS, A.MSOSO�, 
       INTEGER(A.SEL0006) AS FECHACRT, INTEGER(A.SEL0007) AS FECHAPOS, A.MSOF05, A.MSOCOI, 
       CAST(A.MSOUCD AS INTEGER) AS ESTADO 
  FROM CTL06 A 
 INNER JOIN GUAV1.CONTROL B ON ( B.CONBEX = INTEGER(SUBSTR(TEL2,1,6)) AND B.CONBLN = INTEGER(SUBSTR(TEL2,7,4)) 
                                AND B.CONMRS = 0 )

INSERT INTO CTL09 
SELECT INTEGER( TEL1 ) AS PADRE, INTEGER( TEL2 ) AS HIJO, 
       INTEGER( DIGITS(B.CONMEX)||DIGITS(B.CONMLN) ) AS NIETO, A.CICLO
  FROM CTL07 A 
 INNER JOIN GUAV1.CONTROL B ON ( B.CONBEX = INTEGER(SUBSTR(TEL2,1,6)) 
                                AND B.CONBLN = INTEGER(SUBSTR(TEL2,7,4)) AND B.CONMRS = 0 )

INSERT INTO CTL10 
SELECT * 
  FROM CTL08 A 
 WHERE A.FECHAPOS = ( SELECT MAX(Q.FECHAPOS) FROM CTL08 Q WHERE A.PADRE=Q.PADRE AND A.HIJO=Q.HIJO AND A.NIETO=Q.NIETO )

INSERT INTO CTL12 
SELECT * FROM CTL10 

INSERT INTO CTL12 
( PADRE, HIJO, NIETO, CICLO ) 
SELECT * FROM CTL09

INSERT INTO CTL13 
SELECT C.*,S.MSOSO� AS OS 
FROM CTL12 C 
LEFT OUTER JOIN GUAV1.SVORD S ON ( NIETO=MSOPH� AND MSOUCD=0 AND MSOSTS<>'D' ) 

-- Sixbell
INSERT INTO JL637879.SIXRECREP1 
SELECT B.TELEXC, B.TELLIN, B.TELFRE, B.TELTRE, B.TELHPS 
  FROM GUAV1.BLCTEL B 
 WHERE B.TELSTS='1' AND B.TELHPS IN ('svc','rvc') 
   AND TELNAM NOT IN ( SELECT MCHCEN FROM GUAV1.SVMICEHFC )
   AND B.TELFRE BETWEEN 20130501 AND 20130531

INSERT INTO JL637879.SIXRECREP2 
SELECT DISTINCT A.TELEXC, A.TELLIN, A.TELFRE, A.TELTRE, A.TELHPS 
  FROM JL637879.SIXRECREP1 A 
 WHERE A.TELFRE||DIGITS(A.TELTRE) IN ( 
		SELECT MAX(B.TELFRE||DIGITS(B.TELTRE)) 
		  FROM JL637879.SIXRECREP1 B 
		 WHERE A.TELEXC=B.TELEXC AND A.TELLIN=B.TELLIN) 
   AND A.TELHPS='rvc'

DELETE FROM JL637879.SIXRECREP2 WHERE TELHPS='svc'

INSERT INTO JL637879.SIXRECREP3 
SELECT * FROM GUARDBV1.SVHISTCIC A 
 WHERE A.SVHISNOS = ( SELECT MAX(B.SVHISNOS) FROM GUARDBV1.SVHISTCIC B WHERE B.SVHISTEL=A.SVHISTEL ) 
   AND SVHISFEC>0

INSERT INTO JL637879.SIXRECREP4	
SELECT TELEXC,TELLIN,TELFRE,TELTRE,TELHPS,SVHISTOS,SVHISNOS,SVHISFEC,SVHISTEL,'' 
FROM JL637879.SIXRECREP2 R 
LEFT OUTER JOIN JL637879.SIXRECREP3 S ON ( INTEGER(DIGITS(TELEXC)||DIGITS(TELLIN)) = SVHISTEL )

DELETE FROM JL637879.SIXRECREP4 
WHERE SVHISTOS IS NULL OR SVHISTOS IN ('BA')

UPDATE JL637879.SIXRECREP4 SET ESPECIAL='BLTSIX' 
WHERE TELEXC*10000+TELLIN IN ( SELECT GARITA FROM JL637879.GARITAS)