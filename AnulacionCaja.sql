CREATE TABLE PASO.ANUCAJAS (
TIENDA CHARACTER (3),
CAJA CHARACTER (10),
SEC NUMERIC (5),
CONCEPTO CHARACTER (11),
TPO_PAGO NUMERIC (2),
MONTO DECIMAL(11,2),
FORMA_PAGO CHARACTER (3),
USUARIO CHARACTER (10),
NOMBRE VARCHAR (40)
)

INSERT INTO PASO.ANUCAJAS
SELECT CJANUOFI TIENDA, CJANUCAJ CAJA, CJANUSEC SEC,CJANUCON
CONCEPTO,CJANUTIP  TPO_PAGO,
CJANUIMP MONTO, CJANUFOR FORMA_PAGO,
CJANUUSU USUARIO, LTRIM(B.USRMH2) NOMBRE
FROM GUAV1.CJANULA A
LEFT OUTER JOIN GUAV1.MNUSER B ON A.CJANUUSU=B.USRID
WHERE CJANUSEC<>0 AND CJANUFEC=20160408
AND CJANUOFI NOT IN(SELECT COCOD FROM GUAV1.SVCOIC WHERE
COCSTI <>'A') AND SUBSTR(CJANUCON,1,5)<>'TOTAL'
GROUP BY CJANUOFI, CJANUCAJ, CJANUSEC,CJANUCON,CJANUTIP,CJANUIMP,
CJANUFOR,CJANUUSU, B.USRMH2
ORDER BY CJANUOFI

-- ODBC
CREATE TABLE PASO.ANUCAJAS ( TIENDA CHARACTER (3), CAJA CHARACTER (10), SEC NUMERIC (5), CONCEPTO CHARACTER (11), TPO_PAGO NUMERIC (2), MONTO DECIMAL(11,2), FORMA_PAGO CHARACTER (3), USUARIO CHARACTER (10), NOMBRE VARCHAR (40) )
INSERT INTO PASO.ANUCAJAS SELECT CJANUOFI TIENDA, CJANUCAJ CAJA, CJANUSEC SEC,CJANUCON CONCEPTO,CJANUTIP  TPO_PAGO, CJANUIMP MONTO, CJANUFOR FORMA_PAGO, CJANUUSU USUARIO, LTRIM(B.USRMH2) NOMBRE FROM GUAV1.CJANULA A LEFT OUTER JOIN GUAV1.MNUSER B ON A.CJANUUSU=B.USRID WHERE CJANUSEC<>0 AND CJANUFEC="+sAyer+" AND CJANUOFI NOT IN(SELECT COCOD FROM GUAV1.SVCOIC WHERE COCSTI <>'A') AND SUBSTR(CJANUCON,1,5)<>'TOTAL' GROUP BY CJANUOFI, CJANUCAJ, CJANUSEC,CJANUCON,CJANUTIP,CJANUIMP,CJANUFOR,CJANUUSU, B.USRMH2 ORDER BY CJANUOFI