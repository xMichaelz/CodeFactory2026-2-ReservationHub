import base64, re, html, os
D=os.path.dirname(__file__)+'/icons/'
COL={'react':'61DAFB','springboot':'6DB33F','postgresql':'4169E1','docker':'2496ED','nginx':'009639',
 'githubactions':'2088FF','github':'181717','sonarqubecloud':'126ED3','openjdk':'E76F00','swagger':'85EA2D',
 'postman':'FF6C37','googlechrome':'4285F4','flyway':'CC0200','vite':'9135FF'}
def uri(n):
    s=open(D+n+'.svg').read()
    s=s.replace('<svg ','<svg width="24" height="24" fill="#%s" '%COL[n],1)
    return 'data:image/svg+xml,'+base64.b64encode(s.encode()).decode()
cells=[]; nid=[1]
def new():
    nid[0]+=1; return 'c%d'%nid[0]
def add(value,style,x,y,w,h,parent='1',vertex=True):
    i=new()
    cells.append('<mxCell id="%s" value="%s" style="%s" vertex="1" parent="%s"><mxGeometry x="%d" y="%d" width="%d" height="%d" as="geometry"/></mxCell>'%(i,html.escape(value,quote=True),style,parent,x,y,w,h))
    return i
def icon(n,x,y,s=48,label='',parent='1'):
    st='shape=image;aspect=fixed;imageAspect=0;verticalLabelPosition=bottom;verticalAlign=top;labelBackgroundColor=none;fontSize=11;fontColor=#333333;html=1;image=%s;'%uri(n)
    return add(label,st,x,y,s,s,parent)
def aws(shape,x,y,s,label,parent='1',fill='#232F3E'):
    st='sketch=0;outlineConnect=0;fontColor=#232F3E;fillColor=%s;strokeColor=none;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;fontSize=12;fontStyle=1;aspect=fixed;pointerEvents=1;shape=mxgraph.aws4.%s;'%(fill,shape)
    return add(label,st,x,y,s,s,parent)
def box(label,x,y,w,h,fill,stroke,parent='1',dashed=0,font=13,rounded=1,valign='top'):
    st='rounded=%d;arcSize=4;whiteSpace=wrap;html=1;fillColor=%s;strokeColor=%s;strokeWidth=2;dashed=%d;verticalAlign=%s;align=left;spacingLeft=12;spacingTop=6;fontSize=%d;fontColor=#1F2937;container=1;collapsible=0;'%(rounded,fill,stroke,dashed,valign,font)
    return add(label,st,x,y,w,h,parent)
def node3d(label,x,y,w,h,fill,stroke,parent='1'):
    st='shape=cube;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;darkOpacity=0.05;darkOpacity2=0.1;size=14;fillColor=%s;strokeColor=%s;strokeWidth=2;verticalAlign=top;align=left;spacingLeft=10;spacingTop=4;fontSize=13;fontColor=#1F2937;container=1;collapsible=0;'%(fill,stroke)
    return add(label,st,x,y,w,h,parent)
def artifact(label,x,y,w,h,parent):
    st='shape=note;size=12;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#6B7280;fontSize=11;align=left;spacingLeft=8;verticalAlign=middle;fontColor=#111827;'
    return add(label,st,x,y,w,h,parent)
def edge(s,t,label,color='#374151',dashed=0,exit=None,entry=None,pts=None,both=False):
    i=new()
    st='edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;strokeWidth=2;strokeColor=%s;fontSize=11;fontColor=#111827;labelBackgroundColor=#FFFFFF;endArrow=block;endFill=1;dashed=%d;'%(color,dashed)
    if both: st+='startArrow=block;startFill=1;'
    if exit: st+='exitX=%s;exitY=%s;exitDx=0;exitDy=0;'%exit
    if entry: st+='entryX=%s;entryY=%s;entryDx=0;entryDy=0;'%entry
    g='<mxGeometry relative="1" as="geometry">'+('<Array as="points">'+''.join('<mxPoint x="%d" y="%d"/>'%p for p in pts)+'</Array>' if pts else '')+'</mxGeometry>'
    cells.append('<mxCell id="%s" value="%s" style="%s" edge="1" parent="1" source="%s" target="%s">%s</mxCell>'%(i,html.escape(label,quote=True),st,s,t,g))

# Título
add('<b style="font-size:22px">ReservaHub — Diagrama de despliegue (Sprint 2)</b><br><span style="font-size:13px;color:#4B5563">Caso 14 CodeF@ctory UdeA · Monolito modular Spring Boot + SPA React · Docker Compose</span>',
    'text;html=1;align=left;verticalAlign=top;whiteSpace=wrap;',40,20,900,60)

# ---- Dispositivo del usuario
dev=node3d('<b>«device»</b><br>Dispositivo del usuario<br><span style="font-size:11px;color:#6B7280">PC / portátil / móvil</span>',40,150,300,430,'#F9FAFB','#6B7280')
aws('users',20,90,56,'Cliente /<br>Proveedor',dev)
brw=box('<b>«execution environment»</b><br>Navegador web',20,200,250,200,'#EFF6FF','#3B82F6',dev,font=12)
icon('googlechrome',190,8,40,'',brw)
spa=artifact('<b>«artifact»</b> SPA React 19<br>React Router · Vite 8<br>JWT en <i>sessionStorage</i>',15,80,220,62,brw)
icon('react',15,150,36,'',brw); icon('vite',60,150,34,'',brw)

# ---- Servidor Docker host
host=node3d('<b>«device»</b> Servidor de aplicación (Docker host)<br><span style="font-size:11px;color:#6B7280">Linux · Docker Engine + Docker Compose · red bridge <i>reservahub_default</i></span>',420,110,880,560,'#F8FAFC','#2496ED')
icon('docker',800,14,52,'',host)

web=box('<b>«container» reservahub-web</b><br><span style="font-size:11px">imagen nginx:1.27-alpine · puerto 8081 → 80</span>',25,90,260,420,'#ECFDF5','#009639',host,font=12)
icon('nginx',200,52,40,'',web)
artifact('<b>«artifact»</b> dist/ (build estático)<br>index.html + JS/CSS',15,105,230,48,web)
artifact('<b>«config»</b> nginx.conf<br>/ → SPA (try_files)<br>/api/ → proxy_pass app:8080',15,165,230,62,web)
add('Sirve la SPA y actúa como<br><b>reverse proxy</b> hacia la API:<br>el navegador solo habla con<br>un origen (sin CORS en prod).','text;html=1;fontSize=11;align=left;verticalAlign=top;whiteSpace=wrap;fontColor=#065F46;',15,250,230,70,web)
icon('react',15,340,36,'',web)

app=box('<b>«container» reservahub-app</b><br><span style="font-size:11px">imagen eclipse-temurin:25-jre · puerto 8080</span>',315,90,290,420,'#F0FDF4','#6DB33F',host,font=12)
icon('springboot',200,48,40,'',app); icon('openjdk',245,48,36,'',app)
artifact('<b>«artifact»</b> reservahub.jar<br>Spring Boot 4.1 · Java 25',15,100,260,46,app)
mods=add('<b>Monolito modular</b><br>• security (JWT HS256, RBAC, revocación)<br>• user · provider<br>• servicecatalog · schedule · resource<br>• reservation (disponibilidad, reservar,<br>&nbsp;&nbsp;cancelar, reprogramar)<br>• audit · observability (X-Trace-Id)',
  'rounded=1;arcSize=6;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#86EFAC;fontSize=11;align=left;verticalAlign=top;spacingLeft=8;spacingTop=4;',15,158,260,140,app)
artifact('<b>«endpoint»</b> REST /api/v1/**',15,310,260,30,app)
sw=artifact('<b>«endpoint»</b> /swagger-ui.html · /v3/api-docs',15,348,260,30,app)
icon('swagger',238,350,26,'',app)
artifact('<b>«endpoint»</b> /actuator/health',15,386,260,30,app)

db=box('<b>«container» reservahub-db</b><br><span style="font-size:11px">imagen postgres:16-alpine · 5433 → 5432</span>',635,90,225,420,'#EFF6FF','#4169E1',host,font=12)
icon('postgresql',165,48,44,'',db)
schema=add('<b>BD reservahub</b><br>users, providers, services,<br>schedules, blocks, resources,<br>reservations, history,<br>revoked_tokens',
  'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=14;fillColor=#DBEAFE;strokeColor=#4169E1;fontSize=11;fontColor=#1E3A8A;',20,110,185,150,db)
icon('flyway',20,365,32,'',db)
add('Migraciones Flyway<br>V1 … V5',"text;html=1;fontSize=11;align=left;verticalAlign=middle;",58,363,140,36,db)
vol=add('<b>«volume»</b><br>reservahub_pgdata','shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;size=8;fillColor=#F3F4F6;strokeColor=#9CA3AF;dashed=1;fontSize=11;',40,285,145,60,db)

# ---- Pruebas / QA
qa=box('<b>Pruebas manuales / QA</b>',40,620,300,130,'#FFF7ED','#FF6C37',font=12)
pm=icon('postman',25,45,44,'Postman',qa); icon('swagger',125,45,44,'Swagger UI',qa)
aws('client',220,45,44,'Tester',qa,fill='#7C3AED')

# ---- CI/CD
ci=box('<b>Integración continua y calidad de código</b>',420,710,880,150,'#F5F3FF','#7C3AED',dashed=1,font=12)
devp=aws('user',30,50,48,'Equipo de<br>desarrollo',ci,fill='#4B5563')
gh=icon('github',230,45,52,'<b>GitHub</b><br>repos backend y frontend',ci)
ga=icon('githubactions',460,45,52,'<b>GitHub Actions</b><br>mvn verify (JDK 25)',ci)
sq=icon('sonarqubecloud',700,45,52,'<b>SonarQube Cloud</b><br>análisis estático',ci)

# ---- Conexiones
edge(spa,web,'HTTP(S) :8081<br>HTML/JS + REST JSON<br>Authorization: Bearer JWT',color='#2563EB')
edge(web,app,'HTTP :8080<br>proxy /api/**',color='#16A34A')
edge(app,schema,'JDBC :5432<br>JPA',color='#4169E1')
edge(schema,vol,'',color='#9CA3AF',dashed=1)
edge(qa,app,'HTTP :8080 REST JSON (CORS)',color='#EA580C',exit=('1','0.5'),pts=[(880,688)],entry=('0.5','1'))
edge(devp,gh,'git push',color='#4B5563')
edge(gh,ga,'push a main',color='#4B5563')
edge(ga,sq,'sonar scan',color='#4B5563')

# Leyenda
leg=box('<b>Leyenda</b>',1340,110,250,330,'#FFFFFF','#D1D5DB',font=12)
yy=40
for t,c,d in [('Navegador → Nginx','#2563EB',0),('Nginx → API (proxy)','#16A34A',0),('API → PostgreSQL','#4169E1',0),('QA → API directa','#EA580C',0),('CI/CD','#4B5563',0),('Persistencia de datos','#9CA3AF',1)]:
    add('', 'endArrow=block;endFill=1;html=1;strokeWidth=2;strokeColor=%s;dashed=%d;shape=line;'%(c,d),15,yy+8,40,4,leg)
    add(t,'text;html=1;fontSize=11;align=left;verticalAlign=middle;',65,yy,175,20,leg); yy+=28
add('<b>«device»</b> nodo físico/virtual<br><b>«container»</b> contenedor Docker<br><b>«artifact»</b> elemento desplegado<br><br>Puertos: host → contenedor<br>Variables: DB_URL, JWT_SECRET,<br>CORS_ALLOWED_ORIGINS',
    'text;html=1;fontSize=11;align=left;verticalAlign=top;whiteSpace=wrap;',15,yy+5,225,130,leg)

xml='<mxfile host="app.diagrams.net"><diagram id="despliegue" name="Despliegue Sprint 2"><mxGraphModel dx="1600" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1654" pageHeight="900" background="#FFFFFF" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>'+''.join(cells)+'</root></mxGraphModel></diagram></mxfile>'
open('/Users/edudelahoz/Desktop/Desarrollo/CodeFactory2026-2-ReservationHub/docs/entregable-arquisoft-sprint2/diagrama-despliegue.drawio','w').write(xml)
print(len(xml))
