/* auth.jsx — Splash, Login, Business Setup wizard */
const { useState: uaS } = React;

function Splash({ theme, onTheme }) {
  return (
    <div style={{
      width:"100%",height:"100%", background:"var(--ink)", color:"var(--surface)",
      display:"flex",flexDirection:"column",alignItems:"center",justifyContent:"center",
      position:"relative",overflow:"hidden"
    }}>
      <div style={{position:"absolute",inset:0,backgroundImage:"radial-gradient(circle at 30% 20%, rgba(232,101,29,0.35), transparent 55%), radial-gradient(circle at 80% 80%, rgba(232,101,29,0.18), transparent 50%)",opacity:.9}}/>
      {/* engineered grid */}
      <svg style={{position:"absolute",inset:0,width:"100%",height:"100%",opacity:.10}} viewBox="0 0 800 600" preserveAspectRatio="none">
        <defs><pattern id="sp-grid" width="40" height="40" patternUnits="userSpaceOnUse">
          <path d="M40 0H0V40" fill="none" stroke="#F4ECDD" strokeWidth="0.6"/>
        </pattern></defs>
        <rect width="800" height="600" fill="url(#sp-grid)"/>
      </svg>
      <div style={{position:"relative",display:"flex",flexDirection:"column",alignItems:"center",gap:24}}>
        <div style={{display:"flex",alignItems:"center",gap:16}}>
          <Icon.logo size={72}/>
          <div>
            <div style={{fontSize:56,fontWeight:800,letterSpacing:"-0.03em",lineHeight:1}}>NexaPOS</div>
            <div style={{fontFamily:"var(--mono)",fontSize:13,letterSpacing:".18em",marginTop:4,color:"var(--amber)"}}>RETAIL · WORKSHOP PRECISION</div>
          </div>
        </div>
        <div style={{display:"flex",gap:24,marginTop:8,opacity:.7}}>
          <Pill>POS</Pill><Pill>Inventory</Pill><Pill>Accounting</Pill><Pill>Reports</Pill>
        </div>
        <div style={{marginTop:36,display:"flex",alignItems:"center",gap:12}}>
          <div style={{width:160,height:3,background:"rgba(244,236,221,0.15)",borderRadius:99,overflow:"hidden"}}>
            <div style={{width:"72%",height:"100%",background:"var(--amber)"}}/>
          </div>
          <span style={{fontFamily:"var(--mono)",fontSize:11,letterSpacing:".12em",color:"rgba(244,236,221,0.6)"}}>SYNCING LEDGER · 14 OF 19</span>
        </div>
        <div style={{position:"absolute",bottom:-220,fontFamily:"var(--mono)",fontSize:11,letterSpacing:".2em",color:"rgba(244,236,221,0.5)"}}>v 2.4 · BUILD 0526</div>
      </div>
    </div>
  );
}
function Pill({children}) {
  return <span style={{padding:"4px 12px",border:"1px solid rgba(244,236,221,0.25)",borderRadius:99,fontSize:12,fontWeight:600,letterSpacing:".04em"}}>{children}</span>;
}

function Login({ theme, onTheme }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      {/* Left side */}
      <div style={{flex:1,display:"flex",alignItems:"center",justifyContent:"center",padding:40,backgroundImage:"var(--grain)",backgroundBlendMode:"multiply"}}>
        <div style={{width:"100%",maxWidth:380,display:"flex",flexDirection:"column",gap:18}}>
          <div style={{display:"flex",alignItems:"center",gap:10}}>
            <Icon.logo size={32}/>
            <div style={{fontSize:20,fontWeight:800,letterSpacing:"-0.02em"}}>NexaPOS</div>
          </div>
          <div>
            <div className="eyebrow">Welcome back</div>
            <h1 style={{margin:"6px 0 6px",fontSize:34,fontWeight:800,letterSpacing:"-0.02em",lineHeight:1.05}}>
              Sign in to your counter.
            </h1>
            <div style={{color:"var(--muted)",fontSize:14}}>Use your shop credentials to open the till.</div>
          </div>

          <div style={{display:"flex",gap:0,background:"var(--raised)",border:"1px solid var(--hairline)",borderRadius:10,padding:4}}>
            <button style={tab(true)}>Email</button>
            <button style={tab(false)}>Phone</button>
          </div>

          <div>
            <div className="label">Email</div>
            <div className="field"><Icon.user size={16}/><input defaultValue="sameer@rbtrading.mu"/></div>
          </div>
          <div>
            <div className="label">Password</div>
            <div className="field"><input type="password" defaultValue="•••••••••••"/><span style={{fontSize:11,color:"var(--muted)",cursor:"pointer"}}>SHOW</span></div>
          </div>
          <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",fontSize:12}}>
            <label style={{display:"inline-flex",alignItems:"center",gap:6,cursor:"pointer"}}>
              <input type="checkbox" defaultChecked style={{accentColor:"var(--amber)"}}/> Keep me signed in
            </label>
            <a style={{color:"var(--amber)",fontWeight:600,cursor:"pointer"}}>Forgot password?</a>
          </div>

          <button className="btn btn-primary btn-lg">Sign in · open till <Icon.arrow_r size={18}/></button>

          <div style={{display:"flex",alignItems:"center",gap:10,color:"var(--muted)",fontSize:12,margin:"4px 0"}}>
            <div style={{flex:1,height:1,background:"var(--hairline)"}}/> OR <div style={{flex:1,height:1,background:"var(--hairline)"}}/>
          </div>
          <button className="btn btn-secondary btn-lg">
            <span style={{width:20,height:20,borderRadius:4,background:"var(--ink)",color:"var(--amber)",display:"inline-flex",alignItems:"center",justifyContent:"center",fontWeight:800,fontSize:11}}>RB</span>
            Continue with shop SSO
          </button>

          <div style={{fontSize:12,color:"var(--muted)",textAlign:"center",marginTop:8}}>
            New to NexaPOS? <a style={{color:"var(--amber)",fontWeight:600,cursor:"pointer"}}>Create a business</a>
          </div>
        </div>
      </div>

      {/* Right — feature panel */}
      <div style={{flex:1.1,background:"var(--ink)",color:"var(--surface)",position:"relative",overflow:"hidden",padding:40,display:"flex",flexDirection:"column",justifyContent:"space-between"}}>
        <div style={{position:"absolute",inset:0,backgroundImage:"radial-gradient(circle at 80% 0%, rgba(232,101,29,0.45), transparent 50%), radial-gradient(circle at 0% 100%, rgba(232,101,29,0.12), transparent 50%)"}}/>
        <svg style={{position:"absolute",inset:0,width:"100%",height:"100%",opacity:.06}} viewBox="0 0 800 800" preserveAspectRatio="none">
          <defs><pattern id="lg-grid" width="32" height="32" patternUnits="userSpaceOnUse">
            <path d="M32 0H0V32" fill="none" stroke="#F4ECDD" strokeWidth="0.5"/>
          </pattern></defs><rect width="800" height="800" fill="url(#lg-grid)"/>
        </svg>

        <div style={{position:"relative",display:"flex",justifyContent:"space-between",alignItems:"flex-start"}}>
          <button className="btn btn-ghost btn-sm" style={{color:"var(--surface)",borderColor:"rgba(255,255,255,0.18)",background:"transparent",border:"1px solid rgba(255,255,255,0.18)"}}>
            EN · Rs MUR <Icon.chev_d size={14}/>
          </button>
          <button onClick={onTheme} className="btn btn-ghost btn-sm" style={{color:"var(--surface)",border:"1px solid rgba(255,255,255,0.18)"}}>
            {theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>} {theme==="light"?"Counter":"Daylight"}
          </button>
        </div>

        <div style={{position:"relative",maxWidth:480}}>
          <div style={{fontSize:13,letterSpacing:".18em",color:"var(--amber)",fontWeight:600}}>WORKSHOP PRECISION · 2.4</div>
          <h2 style={{fontSize:42,fontWeight:800,letterSpacing:"-0.025em",lineHeight:1.05,marginTop:14}}>
            The till you can hear<br/>tick when it’s fast.
          </h2>
          <p style={{fontSize:15,opacity:.8,marginTop:14,lineHeight:1.5}}>
            Sub-second product lookup, offline-first ledger, and reports your accountant will actually accept. Built for hardware-store counters.
          </p>

          <div style={{display:"flex",gap:12,marginTop:28}}>
            <Stat2 v="1.2s" l="Add → charge"/>
            <Stat2 v="62" l="SKUs synced"/>
            <Stat2 v="99.97%" l="Uptime"/>
          </div>
        </div>

        <div style={{position:"relative",display:"flex",alignItems:"center",justifyContent:"space-between"}}>
          <div style={{display:"flex",alignItems:"center",gap:10}}>
            <div style={{width:38,height:38,borderRadius:99,background:"var(--amber-soft)",color:"var(--amber-press)",display:"flex",alignItems:"center",justifyContent:"center",fontWeight:800,fontFamily:"var(--mono)"}}>RB</div>
            <div>
              <div style={{fontSize:13,fontWeight:700}}>“Closes the day-book in 4 minutes flat.”</div>
              <div style={{fontSize:11,opacity:.6}}>Ramesh B., Owner · Quincaillerie RB Trading</div>
            </div>
          </div>
          <div style={{fontFamily:"var(--mono)",fontSize:11,letterSpacing:".14em",opacity:.5}}>©2026 · NEXAPOS LTD · MU</div>
        </div>
      </div>
    </div>
  );
}
function tab(on){return{flex:1,height:36,border:0,borderRadius:8,cursor:"pointer",background:on?"var(--ink)":"transparent",color:on?"var(--surface)":"var(--ink)",fontWeight:700,fontSize:13};}
function Stat2({v,l}){return(<div><div className="num" style={{fontSize:24,fontWeight:800,letterSpacing:"-0.02em"}}>{v}</div><div style={{fontSize:11,opacity:.65,letterSpacing:".06em",textTransform:"uppercase",fontWeight:600}}>{l}</div></div>);}

/* ============================================================
   Business setup wizard
   ============================================================ */
function BusinessSetup({ theme, onTheme }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <div style={{width:280,background:"var(--ink)",color:"var(--surface)",padding:"28px 24px",display:"flex",flexDirection:"column",justifyContent:"space-between"}}>
        <div>
          <div style={{display:"flex",alignItems:"center",gap:10,marginBottom:30}}>
            <Icon.logo size={28}/>
            <div style={{fontSize:18,fontWeight:800,letterSpacing:"-0.02em"}}>NexaPOS</div>
          </div>
          <div style={{fontSize:12,letterSpacing:".14em",color:"var(--amber)",fontWeight:600}}>FIRST-RUN SETUP</div>
          <h2 style={{margin:"8px 0 22px",fontSize:24,fontWeight:800,letterSpacing:"-0.02em",lineHeight:1.15}}>Let’s set up the shop.</h2>
          <div style={{display:"flex",flexDirection:"column",gap:8}}>
            {[
              {n:1,t:"Business identity",on:false,done:true},
              {n:2,t:"Currency & locale",on:false,done:true},
              {n:3,t:"VAT / Tax",on:true,done:false},
              {n:4,t:"Opening balance",on:false,done:false},
              {n:5,t:"Staff & permissions",on:false,done:false},
              {n:6,t:"Done",on:false,done:false},
            ].map(s=>(
              <div key={s.n} style={{display:"flex",alignItems:"center",gap:10, padding:"8px 10px", borderRadius:8, background: s.on?"rgba(255,255,255,0.06)":"transparent"}}>
                <span style={{
                  width:24,height:24,borderRadius:99,display:"flex",alignItems:"center",justifyContent:"center",
                  background: s.done?"var(--amber)":s.on?"transparent":"transparent",
                  border: s.done?"none":"1.5px solid "+(s.on?"var(--amber)":"rgba(244,236,221,0.25)"),
                  color: s.done?"#fff":(s.on?"var(--amber)":"rgba(244,236,221,0.6)"),
                  fontFamily:"var(--mono)",fontWeight:700,fontSize:11
                }}>{s.done?<Icon.check size={12}/>:s.n}</span>
                <span style={{fontSize:14,fontWeight:s.on?700:500, opacity: s.on||s.done?1:.7}}>{s.t}</span>
              </div>
            ))}
          </div>
        </div>
        <div style={{fontSize:12,opacity:.6}}>You can change everything later in Settings.</div>
      </div>

      <div style={{flex:1,display:"flex",flexDirection:"column",overflow:"hidden"}}>
        <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",padding:"18px 28px",borderBottom:"1px solid var(--hairline)",background:"var(--surface)"}}>
          <div>
            <div className="eyebrow">Step 3 of 6</div>
            <h1 style={{margin:"4px 0 0",fontSize:22,fontWeight:700,letterSpacing:"-0.015em"}}>VAT & tax configuration</h1>
          </div>
          <div style={{display:"flex",gap:8}}>
            <button className="btn btn-ghost btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-ghost">Skip for now</button>
          </div>
        </div>

        <div className="scroll" style={{flex:1,overflow:"auto",padding:"24px 28px"}}>
          <div style={{maxWidth:760,margin:"0 auto",display:"flex",flexDirection:"column",gap:18}}>
            <div className="card-machined" style={{padding:22}}>
              <div className="eyebrow">Are you VAT registered?</div>
              <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:12,marginTop:10}}>
                <button style={radio(true)}>
                  <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
                    <span style={{fontWeight:700}}>Yes, VAT-registered</span>
                    <Icon.check size={18}/>
                  </div>
                  <div style={{fontSize:12,color:"var(--muted)",marginTop:4}}>Mauritius standard rate · 15%</div>
                </button>
                <button style={radio(false)}>
                  <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
                    <span style={{fontWeight:700}}>Not VAT-registered</span>
                    <span style={{width:16,height:16,borderRadius:99,border:"2px solid var(--hairline)"}}/>
                  </div>
                  <div style={{fontSize:12,color:"var(--muted)",marginTop:4}}>Sales below threshold</div>
                </button>
              </div>
            </div>

            <div className="card-machined" style={{padding:22}}>
              <div className="eyebrow" style={{marginBottom:12}}>VAT details</div>
              <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:12}}>
                <Field label="VAT number" v="VAT20188822" mono/>
                <Field label="BRN" v="C20177445" mono/>
                <Field label="Standard rate" v="15.00 %" mono/>
                <Field label="Reduced rate (optional)" v="—"/>
              </div>
              <div style={{marginTop:14, padding:"12px 14px", background:"var(--amber-tint)", border:"1px solid var(--amber-soft)", borderRadius:10, display:"flex", gap:10, alignItems:"flex-start"}}>
                <Icon.bell size={16} style={{color:"var(--amber-press)",flexShrink:0,marginTop:2}}/>
                <div style={{fontSize:13,color:"var(--graphite)",lineHeight:1.4}}>
                  Prices on shelves are usually <strong>tax-inclusive</strong> in Mauritius retail. Your default for new products will be set accordingly — you can change it per item.
                </div>
              </div>
            </div>

            <div className="card-machined" style={{padding:22}}>
              <div className="eyebrow" style={{marginBottom:12}}>Default pricing display</div>
              <div style={{display:"flex",gap:8}}>
                {[{l:"Inclusive",sub:"Rs 1,150"},{l:"Exclusive",sub:"Rs 1,000 + VAT"},{l:"Both",sub:"Show both lines"}].map((o,i)=>(
                  <button key={i} style={{...radio(i===0),flex:1, padding:"14px 14px"}}>
                    <div style={{fontWeight:700,fontSize:14}}>{o.l}</div>
                    <div className="num" style={{fontSize:12,color:"var(--muted)",marginTop:4}}>{o.sub}</div>
                  </button>
                ))}
              </div>
            </div>

            <div style={{display:"flex",justifyContent:"space-between"}}>
              <button className="btn btn-secondary">‹ Back</button>
              <div style={{display:"flex",gap:10}}>
                <button className="btn btn-ghost">Skip</button>
                <button className="btn btn-primary">Continue · Opening balance <Icon.arrow_r size={16}/></button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
function radio(on){return{padding:"14px 16px",borderRadius:12,border:on?"1.5px solid var(--ink)":"1px solid var(--hairline)",background:on?"var(--raised)":"var(--raised-2)",cursor:"pointer",textAlign:"left",color:"var(--ink)"};}

Object.assign(window, { Splash, Login, BusinessSetup });
