function n(e){if(!e)return 0;switch(e.status){case"FINALIZED":return 2;case"RUNNING":return e.dryRun?3:4;case"STOPPED":return 3;default:return e.passRate>0?1:0}}export{n as p};
